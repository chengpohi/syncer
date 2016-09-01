package com.github.chengpohi

import com.github.chengpohi.init.Bootstrap
import com.github.chengpohi.registry.ObjectRegistry
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
    val bootstrap: Bootstrap = new Bootstrap(ObjectRegistry)
    bootstrap.init
    bootstrap.start(config)
  }
}
