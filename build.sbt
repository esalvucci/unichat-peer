name := "unichat"

version := "0.1"

scalaVersion in ThisBuild:= "2.11.8"
lazy val akkaVersion = "2.5.12"

/*
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
*/

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies ++= Seq("io.swagger" % "swagger-core" % "1.5.8",
"com.wordnik.swagger" %% "swagger-async-httpclient" % "0.3.5",
"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.2",
"com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.9.2",
"com.sun.jersey" % "jersey-core" % "1.19.4",
"com.sun.jersey" % "jersey-client" % "1.19.4",
"com.sun.jersey.contribs" % "jersey-multipart" % "1.19.4",
"org.jfarcand" % "jersey-ahc-client" % "1.0.5",
"joda-time" % "joda-time" % "2.9.9",
"org.joda" % "joda-convert" % "1.9.2")
