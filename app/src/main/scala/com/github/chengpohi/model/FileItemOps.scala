package com.github.chengpohi.model

import java.io.File
import java.nio.file.{Path, Paths}

import com.github.chengpohi.config.AppConfig

/**
  * syncer
  * Created by chengpohi on 9/1/16.
  */
object FileItemOps {
  implicit class FileItemOp(fileItem: FileItem) {
    def toFile: File = new File(AppConfig.SYNC_PATH + "/" + fileItem.name)
    def toTmpFile: File = new File(AppConfig.SYNC_PATH + "/" + fileItem.name + ".sending")
    def toPath: Path = Paths.get(AppConfig.SYNC_PATH + "/" + fileItem.name)
  }
}
