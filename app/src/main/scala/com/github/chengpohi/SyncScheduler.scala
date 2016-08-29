package com.github.chengpohi

import java.io.FileWriter
import java.nio.file.Paths

import akka.actor.ActorRef
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.{FileTable, FileTableDAO, PathReader}
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
trait SyncScheduler extends Runnable {
  lazy val LOGER = LoggerFactory.getLogger(getClass.getName)
  val fileTableDAO: FileTableDAO
  val pathReader: PathReader
  val ac: ActorRef
  override def run(): Unit = {
    val newest: FileTable = fileTableDAO.newest
    LOGER.info(s"Update file table {}", newest)
    ac ! newest
  }
}

object SyncScheduler {
  implicit val formats = org.json4s.DefaultFormats
  def apply(pr: PathReader, ft: FileTableDAO, a: ActorRef): SyncScheduler = {
    new SyncScheduler {
      override val pathReader: PathReader = pr
      override val fileTableDAO: FileTableDAO = ft
      override val ac: ActorRef = a
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          val f = write(fileTableDAO.get)
          LOGER.info("Write Records: " + f)
          val fileWriter: FileWriter = new FileWriter(Paths.get(AppConfig.RECORD_FILE).toFile)
          fileWriter.write(f)
          fileWriter.flush()
          fileWriter.close()
        }
      })
    }
  }
}
