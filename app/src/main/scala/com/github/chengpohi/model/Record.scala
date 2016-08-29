package com.github.chengpohi.model

import java.io.File

import com.github.chengpohi.config.AppConfig

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */

case class Record(name: String, md5: String, status: Option[String] = Some("exist")) {
  def toFile: File = new File(AppConfig.SYNC_PATH + "/" + name)
  def toTmpFile: File = new File(AppConfig.SYNC_PATH + "/" + name + ".sending")
}
