package com.github.chengpohi

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig
import akka.util.ByteString
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.FileTable
import com.github.chengpohi.model.FileItem
import com.github.chengpohi.repository.RepositoryService

import scala.concurrent.duration._

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
case class DeleteFile(rs: List[FileItem])
case class NewFile(rs: List[FileItem])
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
    case d: DeleteFile => {
      workerRouter.tell(ConsistentHashableEnvelope(d, d), self)
    }
    case n: NewFile => {
      workerRouter.tell(ConsistentHashableEnvelope(n, n), self)
    }
    case f: FileTable => {
      workerRouter.tell(ConsistentHashableEnvelope(f, f), self)
    }
  }
}

class SyncWorker extends Actor with ActorLogging {
  override def receive: Receive = {
    case d: DeleteFile =>
      log.info("delete files {}", d.rs)
      d.rs.map(_.toFile).filter(_.exists()).foreach(file => {
        file.delete() match {
          case true => log.info("delete file {} success", file.getName)
          case false => log.info(s"delete file {} fail", file.getName)
        }
      })
    case n: NewFile => {
      n.rs.filter(!_.toFile.exists()).foreach(r => {
        log.info(s"new files {}", r.name)
        sender() ! RequestFile(r)
      })
    }
    case n: RequestFile => {
      val stream: FileInputStream = new FileInputStream(n.r.toFile)
      val bytes: Array[Byte] = Array.ofDim[Byte](512)
      while (stream.read(bytes) != -1) {
        sender() ! SendFile(n.r, ByteString(bytes))
      }
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
