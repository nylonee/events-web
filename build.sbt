name := "events-web"
organization := "net.pawel"

version := "1.0-SNAPSHOT"

lazy val events = (project in file("events"))

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .aggregate(events)
  .dependsOn(events)

scalaVersion := "2.13.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.8.18"

enablePlugins(JavaAppPackaging)

//Universal / javaOptions ++= Seq(
//  "-J--add-opens java.base/java.lang=ALL-UNNAMED"
//)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "net.pawel.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "net.pawel.binders._"
