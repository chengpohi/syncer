package com.github.chengpohi.service

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig
import akka.util.ByteString
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.{Create, Delete, Diff, Repository}
import com.github.chengpohi.model.FileItem
import com.github.chengpohi.registry.ObjectRegistry
import com.github.chengpohi.repository.RepositoryService

import scala.concurrent.duration._

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
case class DeleteFile(rs: List[FileItem])
case class RequestFile(r: FileItem)
case class SendFile(r: FileItem, bytes: ByteString)

class SyncService(repositoryService: RepositoryService) extends Actor with ActorLogging {
  implicit val executor = context.system.dispatcher

  val cluster = Cluster(context.system)
  val workerRouter = context.actorOf(FromConfig.props(Props(classOf[SyncWorker])),
    name = "workerRouter")

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

    context.system.scheduler.schedule(
      initialDelay = Duration(1, TimeUnit.SECONDS),
      interval = Duration(AppConfig.INTERVAL, TimeUnit.SECONDS),
      runnable = SyncScheduler(repositoryService, self))
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case rs: Repository => {
      log.info("repository: {}", rs)
      val aggregator = context.actorOf(Props(
        classOf[SyncAggregator]))
      workerRouter.tell(ConsistentHashableEnvelope(rs, rs), aggregator)
    }
  }
}

class SyncWorker extends Actor with ActorLogging {
  val service: RepositoryService = ObjectRegistry.repositoryService

  override def receive: Receive = {
    case re: Repository => {
      val merge: Diff = service.merge(re)
      sender() ! merge
    }
    case n: RequestFile => {
      val stream: FileInputStream = new FileInputStream(n.r.toFile)
      val bytes: Array[Byte] = Array.ofDim[Byte](512)
      while (stream.read(bytes) != -1) {
        sender() ! SendFile(n.r, ByteString(bytes))
      }
    }
  }
}

class SyncAggregator extends Actor with ActorLogging{
  val service: RepositoryService = ObjectRegistry.repositoryService
  override def receive: Receive = {
    case diff: Diff => {
      log.info("Diff: {}", diff)
      diff.remote.foreach(c => {
        c.op match {
          case Create => {
            if (!c.fileItem.toTmpFile.exists()) {
              sender() ! RequestFile(c.fileItem)
            }
          }
          case Delete => service.mergeDeleteCommit(c)
        }
      })
    }
    case s: SendFile => {
      val toFile: File = s.r.toTmpFile
      if (!toFile.exists()) {
        toFile.createNewFile()
      }
      val fileOutputStream: FileOutputStream = new FileOutputStream(toFile, true)
      fileOutputStream.write(s.bytes.toArray)
      fileOutputStream.close()
    }
  }
}
