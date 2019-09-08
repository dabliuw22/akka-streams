package com.leysoft

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, MergeHub, Sink, Source}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object DynamicHandler extends App {
  implicit val system = ActorSystem("dynamic-handler-system")
  implicit val materializer = ActorMaterializer()

  val logger = Logger(LoggerFactory.getLogger("DynamicHandler"))

  val publisherOne = Source(1 to 10)
  val publisherTwo = Source(100 to 110)

  val dynamicMerge = MergeHub.source[Int]
  val materializerSubscriber = dynamicMerge.to(Sink.foreach[Int] { item => logger.info(s"Item: $item") }).run()
  publisherOne.runWith(materializerSubscriber)
  publisherTwo.runWith(materializerSubscriber)
}
