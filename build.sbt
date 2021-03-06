import java.nio.file.{Files, Paths, StandardCopyOption}

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

val commonSetting = Seq(
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions += "-feature",
  initialCommands in console := "import scalaz._, Scalaz._"
)

lazy val akkaDependencies = Seq(
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.4.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.9",
  "com.typesafe.akka" %% "akka-remote" % "2.4.9",
  "com.typesafe.akka" %% "akka-stream" % "2.4.9",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.9"
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
    version := "0.1-SNAPSHOT"
  )
  .settings(
    mainClass in assembly := Some("com.github.chengpohi.App")
  )
  .aggregate(modules)
  .dependsOn(modules)


lazy val root = project.in(file(".")).aggregate(app).dependsOn(app)

lazy val release = taskKey[Unit]("release syncer")

release := {
  val r = assembly.value
  val f: File = new File("release/syncer/lib/syncer.jar")
  if (f.exists()) {
    f.delete()
  }
  Files.copy(Paths.get("app/src/main/resources/application.conf"), Paths.get("release/syncer/conf/application.conf"), StandardCopyOption.REPLACE_EXISTING)
  f.getParentFile.mkdirs()
  Files.copy(r.toPath, f.toPath)
}

