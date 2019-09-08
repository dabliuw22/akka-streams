package com.leysoft

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.{ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl.{Flow, RestartSource, Sink, Source}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object FaultTolerance extends App {
  implicit val system = ActorSystem("fault-tolerance-system")
  implicit val materializer = ActorMaterializer()

  val logger = Logger(LoggerFactory.getLogger("FaultTolerance"))

  val publisher = Source(1 to 10)
  val map = Flow[Int].map { item => if (item == 7) throw FaultToleranceException() else item }
  val subscriber = Sink.foreach[Int] { item => logger.info(s"Item: $item") }

  val source = publisher.via(map).recover { case _: FaultToleranceException => Int.MinValue }
  //source.to(subscriber).async.run()

  val sourceWithRetry = publisher.via(map).recoverWithRetries(2, {
      case _: FaultToleranceException => Source(10 to 15)
    })
  //sourceWithRetry.to(subscriber).async.run()

  implicit val restartGenerator = () => {
    val random = new SecureRandom().nextInt(10)
    publisher.map {item => if (item == random) throw FaultToleranceException() else item }
  }

  val sourceWithRestart = RestartSource.onFailuresWithBackoff(
    minBackoff = 1 seconds,
    maxBackoff = 30 seconds,
    randomFactor = 0.2)(restartGenerator)
  //sourceWithRestart.runWith(subscriber)

  val sourceWithSupervision = publisher.via(map).withAttributes(
    ActorAttributes.supervisionStrategy {
      case _: FaultToleranceException => Resume
      case _ => Stop
    }
  )
  sourceWithSupervision.runWith(subscriber)
}

case class FaultToleranceException(message: String = "Error") extends RuntimeException
