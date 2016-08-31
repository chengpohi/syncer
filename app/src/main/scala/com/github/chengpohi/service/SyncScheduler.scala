package com.github.chengpohi.service

import akka.actor.ActorRef
import com.github.chengpohi.file.Repository
import com.github.chengpohi.repository.RepositoryService
import org.slf4j.LoggerFactory

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
trait SyncScheduler extends Runnable {
  lazy val LOGER = LoggerFactory.getLogger(getClass.getName)
  val repositoryService: RepositoryService
  val ac: ActorRef
  override def run(): Unit = {
    val repository: Repository = repositoryService.commit
    LOGER.info(s"Update file table {}", repository)
    ac ! repository
  }
}

object SyncScheduler {
  implicit val formats = org.json4s.DefaultFormats
  def apply(rs: RepositoryService, a: ActorRef): SyncScheduler = {
    new SyncScheduler {
      override val repositoryService: RepositoryService = rs
      override val ac: ActorRef = a
    }
  }
}
