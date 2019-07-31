package com.leysoft

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}

//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.language.postfixOps

object Main extends App {
  implicit val system = ActorSystem("system")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 100)
  val sinkOne: Sink[Int, Future[Done]] = Sink foreach[Int] { i =>
    Thread.sleep(2000)
    println(i)
  }
  val sinkTwo: Sink[String, Future[Done]] = Sink foreach[String](println)
  val sinkThree: Sink[Int, Future[Int]] = Sink.reduce[Int] { (a, b) => a + b }
  val flow = Flow[Int] map { i => i + 1 }
  val flowTwo = Flow[Int] map { i =>  s"-$i" }
  val graph = source.to(sinkThree)
  val result = source.runWith(sinkThree)
  /*result.onComplete {
    case Success(value) => println(s"$value")
    case Failure(_) => println(s"Failure")
  }*/
  Source(1 to 1000).throttle(10, 1 seconds)
    .via(flow.buffer(5, OverflowStrategy.backpressure)).async
    .runWith(sinkOne)
  //source.via(flow).via(flowTwo).to(sinkTwo).run()
  //Source(scala.collection.immutable.LazyList.from(1)).runWith(sinkOne)
  /*source.map(i => {
    println(s"A: $i")
    Thread.sleep(500)
    i
  }).async.map(i => {
    println(s"B: $i")
    Thread.sleep(1000)
    i
  }).async.map(i => {
    println(s"C: $i")
    i
  }).async.runWith(Sink.ignore)*/
}
