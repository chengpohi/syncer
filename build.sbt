scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

val commonSetting = Seq(
  version := "1.0",
  scalaVersion := "2.11.8",
  scalacOptions += "-feature",
  initialCommands in console := "import scalaz._, Scalaz._"
)

lazy val akkaDependencies = Seq(
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.1",
  "com.typesafe.akka" %% "akka-remote" % "2.4.1",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.1"
)



val commonDependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.chuusai" %% "shapeless" % "2.3.1",
  "org.scalaz" %% "scalaz-core" % "7.3.0-M4",
  "org.scalaz" %% "scalaz-effect" % "7.3.0-M4",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "com.lihaoyi" %% "fastparse" % "0.3.4",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "log4j" % "log4j" % "1.2.17",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "org.json4s" %% "json4s-jackson" % "3.2.10"
)
libraryDependencies ++= commonDependencies ++ akkaDependencies

lazy val modules = project.in(file("modules"))
  .settings(commonSetting: _*)
  .settings(libraryDependencies ++= commonDependencies ++ akkaDependencies)

lazy val app = project.in(file("app"))
  .settings(commonSetting: _*)
  .settings(libraryDependencies ++= commonDependencies ++ akkaDependencies)
  .settings(
    name := "syncer",
    version := "0.1"
  )
  .aggregate(modules)
  .dependsOn(modules)

