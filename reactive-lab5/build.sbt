name := """reactive-lab5"""

version := "1.2"

scalaVersion := "2.13.3"
val akkaVersion = "2.6.10"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "io.netty" % "netty" % "3.10.6.Final", // for deprecated classic remoting
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion, //as ActorSelection is removed from akka typed
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  //"io.aeron" % "aeron-driver" % "1.30.0",
  //"io.aeron" % "aeron-client" % "1.30.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)
