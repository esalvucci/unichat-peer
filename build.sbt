name := "unichat-peer"

version := "0.1"

scalaVersion in ThisBuild:= "2.11.12"
lazy val akkaVersion = "2.5.12"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % akkaVersion,
"com.typesafe.akka" %% "akka-stream" % akkaVersion,
"com.typesafe.akka" %% "akka-remote" % akkaVersion,
"io.netty" % "netty" % "3.6.3.Final" force(),
"io.swagger" % "swagger-core" % "1.5.8",
"com.wordnik.swagger" %% "swagger-async-httpclient" % "0.3.5",
"com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.2",
"com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.9.2",
"com.sun.jersey" % "jersey-core" % "1.19.4",
"com.sun.jersey" % "jersey-client" % "1.19.4",
"com.sun.jersey.contribs" % "jersey-multipart" % "1.19.4",
"org.jfarcand" % "jersey-ahc-client" % "1.0.5",
"joda-time" % "joda-time" % "2.9.9",
"org.joda" % "joda-convert" % "1.9.2",
"org.scalatest" %% "scalatest" % "3.0.4" % "test",
"junit" % "junit" % "4.12" % "test")
