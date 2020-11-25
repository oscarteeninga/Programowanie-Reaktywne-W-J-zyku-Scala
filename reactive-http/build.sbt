name := """reactive-http"""

version := "1.2"

scalaVersion := "2.12.3"
   
val akkaVersion = "2.5.4"
val akkaHttpVersion = "10.0.10"
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor"  % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion