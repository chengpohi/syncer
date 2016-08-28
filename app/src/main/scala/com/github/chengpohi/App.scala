package com.github.chengpohi

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.PathReader

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
object App {
  val pathReader: PathReader = PathReader.apply(AppConfig.SYNC_PATH)
  val actorSystem = ActorSystem()
  val scheduler = actorSystem.scheduler
  implicit val executor = actorSystem.dispatcher
  def main(args: Array[String]): Unit = {
    scheduler.schedule(
      initialDelay = Duration(1, TimeUnit.SECONDS),
      interval = Duration(AppConfig.INTERVAL, TimeUnit.SECONDS),
      runnable = SyncScheduler.apply(pathReader))
  }
}
