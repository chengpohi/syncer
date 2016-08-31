package com.github.chengpohi.init

import akka.actor.{ActorSystem, Props}
import com.github.chengpohi.repository.RepositoryService
import com.github.chengpohi.service.SyncService
import com.typesafe.config.Config

import scala.language.reflectiveCalls

/**
  * syncer
  * Created by chengpohi on 8/30/16.
  */
class Bootstrap(env: {val repositoryService: RepositoryService}) {
  val rs = env.repositoryService
  def init = {
    rs.commit
  }
  def start(config: Config) = {
    val actorSystem = ActorSystem("Syncer", config)
    actorSystem.actorOf(Props(classOf[SyncService], rs), name = "syncer")
  }
}
