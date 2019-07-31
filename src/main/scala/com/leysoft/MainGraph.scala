package com.leysoft

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.Future

object MainGraph extends App {
  implicit val system = ActorSystem("graph-system")
  implicit val materializer = ActorMaterializer()(system)

  /*
  val input = Source(1 to 10)
  val incrementer = Flow[Int].map { i => i + 1 }
  val multiplier = Flow[Int].map { i => i * 2 }
  val simpleOutput = Sink.foreach[Int](println)
  val output = Sink.foreach[(Int, Int)](println)

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      // para usar el operador ~>
      import GraphDSL.Implicits._
      // fan-out
      val broadcast = builder.add(Broadcast[Int](2))
      // fan-in
      val merge = builder.add(Merge[Int](2))
      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> merge.in(0)
      broadcast.out(1) ~> multiplier ~> merge.in(1)
      merge.out ~> simpleOutput
      ClosedShape
    })
  output.async
  graph.run()*/

  val g = graphWithBroadcastAndZip(Source.single(2), Flow[Int].map { i => println(Thread.currentThread().getName); i > 0 },
    Flow[Int].map {i => println(Thread.currentThread().getName); i % 2 == 0 }, Flow[(Boolean, Boolean)].map { t => t._1 && t._2 },
    Sink.foreach(println))
  g.run()

  def graphWithBroadcastAndZip(in: Source[Int, NotUsed], f1: Flow[Int, Boolean, NotUsed],
                               f2: Flow[Int, Boolean, NotUsed], f3: Flow[(Boolean, Boolean), Boolean, NotUsed],
                               out: Sink[Boolean, Future[Done]]): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL
      .create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))
        val zip = builder.add(Zip[Boolean, Boolean])
        in ~> broadcast
        broadcast.out(0) ~> f1.async ~> zip.in0
        broadcast.out(1) ~> f2.async ~> zip.in1
        zip.out ~>  f3.async ~> out
        ClosedShape
      })
}
