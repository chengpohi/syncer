package com.github.chengpohi

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.{FileTableDAO, PathReader}
import com.github.chengpohi.repository.RepositoryService

/**
  * syncer
  * Created by chengpohi on 8/30/16.
  */
object ObjectRegistry {
  val pathReader: PathReader = PathReader(AppConfig.SYNC_PATH)
  val fileTableDAO = FileTableDAO(pathReader)
  val repositoryService = new RepositoryService(fileTableDAO)
}
