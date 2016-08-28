package com.github.chengpohi

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.PathReader
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
object App {
  val pathReader: PathReader = PathReader.apply(AppConfig.SYNC_PATH)
  def main(args: Array[String]): Unit = {
    val config = args.isEmpty match {
      case true => ConfigFactory.load()
      case false => ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args.head).
        withFallback(ConfigFactory.load())
    }
    val actorSystem = ActorSystem("Syncer", config)
    val scheduler = actorSystem.scheduler
    implicit val executor = actorSystem.dispatcher
    val ac: ActorRef = actorSystem.actorOf(Props[SyncService], name = "syncer")

    scheduler.schedule(
      initialDelay = Duration(1, TimeUnit.SECONDS),
      interval = Duration(AppConfig.INTERVAL, TimeUnit.SECONDS),
      runnable = SyncScheduler.apply(pathReader, ac))
  }
}
