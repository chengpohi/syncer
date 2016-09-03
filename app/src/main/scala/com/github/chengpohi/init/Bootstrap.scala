package com.github.chengpohi.init

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.repository.RepositoryService
import com.github.chengpohi.service.{SyncScheduler, SyncService}
import com.github.chengpohi.tcp.Receiver
import com.typesafe.config.Config

import scala.concurrent.duration.Duration
import scala.language.reflectiveCalls

/**
  * syncer
  * Created by chengpohi on 8/30/16.
  */
class Bootstrap(env: {val repositoryService: RepositoryService}) {
  import scala.concurrent.ExecutionContext.Implicits.global
  val rs = env.repositoryService
  def init = {
    val file: File = new File(AppConfig.SYNC_HISTORY_FOLDER)
    file.exists() match {
      case true =>
      case false => file.mkdir()
    }
    rs.commit()
  }
  def start(config: Config) = {
    val actorSystem = ActorSystem("Syncer", config)
    val syncService: ActorRef = actorSystem.actorOf(Props(classOf[SyncService]), name = "syncer")
    val receiver = new Receiver(actorSystem, AppConfig.HOSTNAME, AppConfig.RECEIVER_PORT)
    actorSystem.scheduler.schedule(
      initialDelay = Duration(20, TimeUnit.SECONDS),
      interval = Duration(AppConfig.INTERVAL, TimeUnit.SECONDS),
      runnable = SyncScheduler(rs, syncService))

  }
}
