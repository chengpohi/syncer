package com.github.chengpohi.service

import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig
import akka.util.ByteString
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.{Commit, Create, Delete, Diff, Repository}
import com.github.chengpohi.model.FileItem
import com.github.chengpohi.registry.ObjectRegistry
import com.github.chengpohi.repository.RepositoryService
import com.github.chengpohi.tcp.Sender

import scala.concurrent.duration._

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
case class DeleteFile(rs: List[FileItem])
case class RequestFile(f: FileItem, dist: Path, c: Commit)
case class SendFile(r: FileItem, bytes: ByteString)
case class SendFileDone(commit: Commit)

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
      val diff: Diff = service.diff(re)
      sender() ! diff
    }
    case n: RequestFile => {
      sender().path.address.host match {
        case Some(host) => {
          new Sender(context.system, host, AppConfig.RECEIVER_PORT, sender(), n.c).send(n.dist)
        }
        case None => log.error("could not send file by host is none: {}", n)
      }
    }
  }
}

class SyncAggregator extends Actor with ActorLogging {
  val service: RepositoryService = ObjectRegistry.repositoryService
  override def receive: Receive = {
    case diff: Diff => {
      log.info("Diff: {}", diff)
      diff.remote.foreach(c => {
        c.op match {
          case Create =>
            if (!c.fileItem.toFile.exists() && !c.fileItem.toTmpFile.exists()) {
              sender() ! RequestFile(c.fileItem, c.fileItem.toTmpFile.toPath, c)
            }
          case Delete => service.mergeDeleteCommit(c)
        }
      })
    }
    case sf: SendFileDone => {
      val tmpPath: Path = sf.commit.fileItem.toTmpFile.toPath
      val distPath: Path = sf.commit.fileItem.toFile.toPath
      Files.move(tmpPath, distPath)
      service.mergeCreateCommit(sf.commit)
    }
  }
}
