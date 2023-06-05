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
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.8.18"
libraryDependencies += "ai.snips" %% "play-mongo-bson" % "0.5.2"
libraryDependencies += "com.google.api-client" % "google-api-client" % "2.0.0"
libraryDependencies += "com.google.oauth-client" % "google-oauth-client-jetty" % "1.34.1"
libraryDependencies += "com.google.apis" % "google-api-services-calendar" % "v3-rev20220715-2.0.0"

enablePlugins(JavaAppPackaging)

//Universal / javaOptions ++= Seq(
//  "-J--add-opens java.base/java.lang=ALL-UNNAMED"
//)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "net.pawel.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "net.pawel.binders._"
