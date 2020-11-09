name := """reactive-lab4"""

version := "1.3"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"               % akkaVersion,
  "com.typesafe.akka"         %% "akka-testkit"             % akkaVersion % "test",
  "com.typesafe.akka"         %% "akka-persistence-typed"   % akkaVersion,
  "com.typesafe.akka"         %% "akka-actor-typed"         % akkaVersion,
  "com.typesafe.akka"         %% "akka-actor-testkit-typed" % akkaVersion % "test",
  "com.typesafe.akka"         %% "akka-persistence"         % akkaVersion,
  "org.scalatest"             %% "scalatest"                % "3.2.2" % "test",
  "org.iq80.leveldb"          % "leveldb"                   % "0.12",
  "org.fusesource.leveldbjni" % "leveldbjni-all"            % "1.8",
  "ch.qos.logback"            % "logback-classic"           % "1.2.3"
)
