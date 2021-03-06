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
  lazy val log = LoggerFactory.getLogger(getClass.getName)
  val repositoryService: RepositoryService
  val ac: ActorRef
  override def run(): Unit = {
    repositoryService.synchronized {
      val repository: Repository = repositoryService.commit()
      ac ! repository
    }
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
