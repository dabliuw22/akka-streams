package com.leysoft

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object SubStream extends App {
  implicit val system = ActorSystem("dynamic-handler-system")
  implicit val materializer = ActorMaterializer()

  val logger = Logger(LoggerFactory.getLogger("SubStream"))

  val publisher = Source(List("Aa", "!", "bB", "*", "Cc", "~"))

  // groupBy
  val groupStream = publisher.groupBy(2, _.length % 2).async
  groupStream.to(Sink.foreach { value => logger.info(s"Value: $value") }).run()
}
