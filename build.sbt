name := "unichat"

version := "0.1"

scalaVersion := "2.11.0"
lazy val akkaVersion = "2.5.12"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.8"
libraryDependencies += "io.spray" % "spray-client" % "1.3.1"
libraryDependencies += "joda-time" % "joda-time" % "2.9.9"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.3"
