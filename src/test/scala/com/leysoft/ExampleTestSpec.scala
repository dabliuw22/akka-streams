package com.leysoft

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.util.Success

class ExampleTestSpec extends TestKit(ActorSystem("TestingAkkaStreams")) with WordSpecLike with BeforeAndAfterAll {

  implicit val materializer = ActorMaterializer()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A Simple Publisher" should {
    "Basic sum() assertion" in {
      val expectValue = 55
      val publisher = Source(1 to 10)
      val result = publisher.runWith(Sink.reduce[Int]{ (a, b) => a + b })
      result.onComplete {
        case Success(value) => assert(value == expectValue)
      }(system.dispatcher)
    }
  }
}
