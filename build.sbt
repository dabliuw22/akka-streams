name := "akka-streams"

version := "0.1"

scalaVersion := "2.13.0"

val akkaParent = "com.typesafe.akka"
val akkaVersion = "2.5.23"

libraryDependencies ++= Seq(
  akkaParent %% "akka-stream" % akkaVersion
)