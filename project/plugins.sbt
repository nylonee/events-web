addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.19")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
