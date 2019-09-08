package com.leysoft

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, Graph, SinkShape, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Success

object MaterializeMainGraph extends App {
  implicit val system = ActorSystem("materialize-graph-system")
  implicit val materializer = ActorMaterializer()
  val logger = Logger(LoggerFactory.getLogger("MaterializeMainGraph"))

  val chainPublisher = Source(List("Scala", "jvm", "Akka", "Kotlin", "java"))
  val printSubscriber = Sink.foreach {
    println
  }
  val countSubscriber = Sink.fold[Int, Int](0) { (count, _) => count + 1 }

  val graphValue: Graph[SinkShape[String], Future[Int]] = GraphDSL
    .create(countSubscriber, printSubscriber)((c, p) => c) {
      implicit builder =>
        (sinkCount, sinkPrint) =>
          import GraphDSL.Implicits._
          val broadcast: UniformFanOutShape[String, String] = builder.add(Broadcast[String](2))
          val filter: FlowShape[String, Int] = builder.add(Flow[String]
            .filter {
              _.length >= 5
            }
            .map {
              _.length
            })
          broadcast.out(0) ~> sinkPrint
          broadcast.out(1) ~> filter ~> sinkCount
          SinkShape(broadcast.in)
    }

  val value: Future[Int] = chainPublisher.runWith(graphValue)
  value.onComplete {
    case Success(value) => logger.info(s"Counter: $value")
    case _ => logger.error(s"Error")
  }(system.dispatcher)

  def graphFlow[A, B](flow: Flow[A, B, _], sink: Sink[B, Future[B]]): Flow[A, B, Future[B]] =
    Flow.fromGraph(GraphDSL.create(sink) { implicit builder =>
      s =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[B](2))
        val f = builder.add(flow)
        f ~> broadcast ~> s
        FlowShape(f.in, broadcast.out(1))
    })

  val flow = graphFlow(Flow[String].filter {
    _.length >= 5
  }.map {
    _.length
  }, countSubscriber)
  val v = chainPublisher.viaMat(flow)(Keep.right).toMat(Sink.ignore)(Keep.left).run()
  v.onComplete {
    case Success(value) => logger.info(s"Counter2: $value")
    case _ => logger.error(s"Error2")
  }(system.dispatcher)

  system.terminate()
}
