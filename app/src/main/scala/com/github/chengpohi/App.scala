package com.github.chengpohi


import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
object App {
  def main(args: Array[String]): Unit = {
    val config = args.isEmpty match {
      case true => ConfigFactory.load()
      case false => ConfigFactory.parseString("akka.remote.netty.tcp.port=" + args.head).
        withFallback(ConfigFactory.load())
    }
    val actorSystem = ActorSystem("Syncer", config)
    actorSystem.actorOf(Props[SyncService], name = "syncer")
  }
}
