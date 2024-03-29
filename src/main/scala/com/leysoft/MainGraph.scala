package com.leysoft

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object MainGraph extends App {
  implicit val system = ActorSystem("graph-system")
  implicit val materializer = ActorMaterializer()(system)
  val logger = Logger(LoggerFactory.getLogger("MainGraph"))

  /*
  val input = Source(1 to 10)
  val incrementer = Flow[Int].map { i => i + 1 }
  val multiplier = Flow[Int].map { i => i * 2 }
  val simpleOutput = Sink.foreach[Int](i => logger.info(s"$i"))
  val output = Sink.foreach[(Int, Int)](i => logger.info(s"(${i._1}, ${i._2})"))

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

  def graphWithBroadcastAndZip(in: Source[Int, NotUsed], f1: Flow[Int, Boolean, NotUsed],
                               f2: Flow[Int, Boolean, NotUsed], f3: Flow[(Boolean, Boolean), Boolean, NotUsed],
                               out: Sink[Boolean, Future[Done]]): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL
      .create() { implicit builder =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2))
        val zip = builder.add(Zip[Boolean, Boolean])
        in ~> broadcast
        broadcast.out(0) ~> f1.async ~> zip.in0
        broadcast.out(1) ~> f2.async ~> zip.in1
        zip.out ~> f3.async ~> out
        ClosedShape
      })

  val g = graphWithBroadcastAndZip(Source.single(2), Flow[Int].map { i =>
    logger.info(s"${Thread.currentThread().getName}")
    i > 0
  }, Flow[Int].map { i =>
    logger.info(s"${Thread.currentThread().getName}");
    i % 2 == 0
  }, Flow[(Boolean, Boolean)].map { t => t._1 && t._2 }, Sink.foreach(i => logger.info(s"$i")))
  //g.run()

  def sourceGraph[A](s1: Source[A, _], s2: Source[A, _]): Source[A, _] =
    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val concat = builder.add(Concat[A](2))
      s1.async ~> concat.in(0)
      s2.async ~> concat.in(1)
      SourceShape(concat.out)
    })

  val s = sourceGraph(Source.single(1), Source.single(2))
  //s.to(Sink.foreach { i => logger.info(s"Data: $i") }).run()

  def flowGraph[A, B](f1: Flow[A, A, _], f2: Flow[A, B, NotUsed]): Flow[A, (A, B), _] =
    Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[A](2))
      val zip = builder.add(Zip[A, B])
      broadcast.out(0) ~> f1 ~> zip.in0
      broadcast.out(1) ~> f2 ~> zip.in1
      FlowShape(broadcast.in, zip.out)
    })

  val f = flowGraph(Flow[Int].map { i => i + 1 }, Flow[Int].map { i => s"N-$i" })
  //Source.single(2).via(f).runWith(Sink.foreach { i => logger.info(s"(${i._1}, ${i._2})") })

  def sinkGraph[A](s1: Sink[A, Future[Done]], s2: Sink[A, Future[Done]]): Sink[A, NotUsed] =
    Sink.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[A](2))
      broadcast ~> s1
      broadcast ~> s2
      SinkShape(broadcast.in)
    })

  val sk = sinkGraph(Sink.foreach[Int](i => logger.info(s"1: $i")),
    Sink.foreach[Int](i => logger.info(s"2: $i")))
  //Source.single(55).runWith(sk)
}
