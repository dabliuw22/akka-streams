package com.leysoft

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, Inlet, Outlet, Shape}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object CustomShape extends App {
  implicit val system = ActorSystem("dynamic-handler-system")
  implicit val materializer = ActorMaterializer()

  val logger = Logger(LoggerFactory.getLogger("CustomShape"))

  val publisherOne = Source(1 to 10)
  val publisherTwo = Source(20 to 30)

  val f = (id: Int) => Sink.foreach[Int] { item => logger.info(s"Consumer$id, item: $item") }

  val graphImp = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val merge = builder.add(Merge[Int](inputPorts = 2))
    val balance = builder.add(Balance[Int](outputPorts = 3))

    merge ~> balance

    Balance2x3(merge.in(0), merge.in(1), balance.out(0), balance.out(1), balance.out(2))
  }

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val consumer1 = builder.add(f(1))
    val consumer2 = builder.add(f(2))
    val consumer3 = builder.add(f(3))

    val balance2x3 = builder.add(graphImp)

    publisherOne ~> balance2x3.in0
    publisherTwo ~> balance2x3.in1
    balance2x3.out0 ~> consumer1
    balance2x3.out1 ~> consumer2
    balance2x3.out2 ~> consumer3
    ClosedShape
  }).run()
}

case class Balance2x3(in0: Inlet[Int], in1: Inlet[Int], out0: Outlet[Int],
                      out1: Outlet[Int], out2: Outlet[Int]) extends Shape {

  override def inlets: Seq[Inlet[_]] = List(in0, in1)

  override def outlets: Seq[Outlet[_]] = List(out0, out1, out2)

  override def deepCopy(): Shape = Balance2x3(in0.carbonCopy(), in1.carbonCopy(),
    out0.carbonCopy(), out1.carbonCopy(), out2.carbonCopy())
}
