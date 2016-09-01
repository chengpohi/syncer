package com.github.chengpohi.model

import java.io.File
import java.nio.file.{Path, Paths}

import com.github.chengpohi.config.AppConfig

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */

case class FileItem(name: String, md5: String) {
  def toFile: File = new File(AppConfig.SYNC_PATH + "/" + name)
  def toTmpFile: File = new File(AppConfig.SYNC_PATH + "/" + name + ".sending")
  def toPath: Path = Paths.get(AppConfig.SYNC_PATH + "/" + name)
}
