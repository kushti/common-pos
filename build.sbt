name := "common-pos"

version := "1.0"

scalaVersion := "2.11.6"

javaOptions += "-XX:MaxPermSize=4096"

licenses := Seq("Public domain / CC0" ->
  url("http://creativecommons.org/publicdomain/zero/1.0/"))

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "org.jliszka" %% "probability-monad" % "1.0.1",
  "com.github.nscala-time" %% "nscala-time" % "2.6.0",
  "com.nativelibs4java" %% "scalaxy-loops" % "0.3.3" % "provided"
)