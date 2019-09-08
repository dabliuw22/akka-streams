package com.leysoft

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object ActorMain extends App {
  implicit val system = ActorSystem("actor-system")
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(2 seconds)
  val logger = Logger(LoggerFactory.getLogger("ActorMain"))
  val source = Source(1 to 10)
  val sink = Sink.foreach[Int] { println }

  class CustomActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case s: String => log.info(s"String: $s")
        sender() ! s
      case i: Int => log.info(s"Int: $i")
        sender() ! i * 2
      case _ => log.error("Error")
    }
  }

  val simpleActor = system.actorOf(Props[CustomActor], "simple-actor")
  val flowActor = Flow[Int].ask[Int](4)(simpleActor)
  //source.via(flowActor).runWith(sink)

  val sourceActor = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
    .to(sink).run()
  sourceActor ! 22

  case object InitStream

  case object AckStream

  case object CompleteStream

  case class FailureStream(error: Throwable)

  class CustomSinkActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case InitStream => log.info("Init Stream")
        sender() ! AckStream
      case CompleteStream => log.info("Complete Stream")
        context.stop(self)
      case FailureStream => log.error("Failure Stream")
      case message => log.info(s"Message: $message")
        sender() ! AckStream
    }
  }

  val customSinkActor = system.actorOf(Props[CustomSinkActor], "sink-actor")
  val sinkActor = Sink.actorRefWithAck[Int](ref = customSinkActor, onInitMessage = InitStream,
    onCompleteMessage = CompleteStream, onFailureMessage = FailureStream, ackMessage = AckStream)
  Source(1 to 10).to(sinkActor).run()
}
