package com.github.chengpohi.config

import com.typesafe.config.ConfigFactory

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
object AppConfig {
  lazy val SYNCER_CONFIG = ConfigFactory.load("application")
  lazy val SYNC_PATH = SYNCER_CONFIG.getConfig("syncer").getString("path")
  lazy val RECORD_FILE = SYNCER_CONFIG.getConfig("syncer").getString("record")
  lazy val INTERVAL = SYNCER_CONFIG.getConfig("syncer").getInt("interval")
}
