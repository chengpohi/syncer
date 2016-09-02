package com.github.chengpohi.config

import java.net.InetSocketAddress

import com.typesafe.config.ConfigFactory

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
object AppConfig {
  lazy val SYNCER_CONFIG = ConfigFactory.load("application")
  lazy val SYNC_PATH = SYNCER_CONFIG.getConfig("syncer").getString("path")
  lazy val INTERVAL = SYNCER_CONFIG.getConfig("syncer").getInt("interval")
  lazy val HISTORY_FILE = SYNC_PATH + "/.history"
  lazy val HOSTNAME = SYNCER_CONFIG.getString("akka.remote.netty.tcp.hostname")
  lazy val SERVER_ADDRESS: InetSocketAddress = new InetSocketAddress(AppConfig.HOSTNAME, 0)
  lazy val RECEIVER_PORT = SYNCER_CONFIG.getInt("syncer.receiver.port")
}
