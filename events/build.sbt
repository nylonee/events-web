name := "events"
organization := "net.pawel"
version := "1.0"

scalaVersion := "2.13.10"

val scalaTestVersion = "3.2.15"

libraryDependencies += "org.scalactic" %% "scalactic" % scalaTestVersion % Test
libraryDependencies += "org.scalatest" %% "scalatest" % scalaTestVersion % Test
libraryDependencies += "com.konghq" % "unirest-java" % "3.14.1"
libraryDependencies += "org.jsoup" % "jsoup" % "1.15.3"
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.4"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0"
libraryDependencies += "javax.inject" % "javax.inject" % "1"
libraryDependencies += "org.mnode.ical4j" % "ical4j" % "4.0.0-beta5"
