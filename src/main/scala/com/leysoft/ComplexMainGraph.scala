package com.leysoft

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanInShape}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import Math.max

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object ComplexMainGraph extends App {
  implicit val system = ActorSystem("complex-graph-system")
  implicit val materializer = ActorMaterializer()
  val logger = Logger(LoggerFactory.getLogger("ComplexMainGraph"))

  val publisher1 = Source.single(1)
  val publisher2 = Source.single(3)
  val publisher3 = Source.single(2)
  val subscriber = Sink.foreach[Int] { i => logger.info(s"$i") }

  val partialGraph = GraphDSL.create() { implicit build =>
    import GraphDSL.Implicits._
    // compare z1.in0 and z1.in1
    val z1 = build.add(ZipWith[Int, Int, Int] {
      max
    })
    // compare z2.in0(z1.out) and z2.in1
    val z2 = build.add(ZipWith[Int, Int, Int] {
      max
    })
    z1.out ~> z2.in0
    UniformFanInShape(z2.out, z1.in0, z1.in1, z2.in1)
  }

  val graph = RunnableGraph.fromGraph(GraphDSL.create(subscriber) { implicit build =>
    sink =>
      import GraphDSL.Implicits._
      // use partialGraph
      val partial = build.add(partialGraph)
      publisher1 ~> partial.in(0)
      publisher2 ~> partial.in(1)
      publisher3 ~> partial.in(2)
      partial.out ~> sink
      ClosedShape
  })
  graph.run()

  system.terminate()
}
