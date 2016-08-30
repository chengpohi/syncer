package com.github.chengpohi

import java.io.{FileNotFoundException, FileWriter}
import java.nio.file.Paths
import java.util.Date

import akka.actor.{ActorSystem, Props}
import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.{Commit, Create, Delete, FileTableDAO, OperationSerializer, PathReader, Repository}
import com.typesafe.config.Config
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

import scalaz.Scalaz._
import scalaz.effect.IO

/**
  * syncer
  * Created by chengpohi on 8/30/16.
  */
object Bootstrap {
  implicit val formats = org.json4s.DefaultFormats + OperationSerializer
  val pathReader: PathReader = PathReader(AppConfig.SYNC_PATH)
  val fileTableDAO = FileTableDAO(pathReader)
  def init = {
    val fileItems = fileTableDAO.getFiles
    val repository = readRepository
    val updatedRepository = repository match {
      case None => {
        val commits: List[Commit] = fileItems.map(f => Commit(new Date, Create, f))
        Repository(fileItems, commits)
      }
      case Some(rep) => {
        val deleteFiles = rep.fileItems.diff(fileItems)
        val newFiles = fileItems.diff(rep.fileItems)
        val deleteCommits: List[Commit] = deleteFiles.map(f => Commit(new Date, Delete, f))
        val createCommits: List[Commit] = newFiles.map(f => Commit(new Date, Create, f))
        Repository(fileItems, rep.commits ::: deleteCommits ::: createCommits)
      }
    }
    writeRepository(updatedRepository)
    updatedRepository
  }

  def start(config: Config) = {
    val actorSystem = ActorSystem("Syncer", config)
    actorSystem.actorOf(Props[SyncService], name = "syncer")
  }

  def readRepository: Option[Repository] = {
    val io = IO {
      scala.io.Source.fromFile(AppConfig.HISTORY_FILE).mkString
    }
    try {
      io.map(raw => parse(raw).extract[Repository]).unsafePerformIO().some
    } catch {
      case e: FileNotFoundException => None
    }
  }

  def writeRepository(repository: Repository) = {
    val f = write(repository)
    val fileWriter: FileWriter = new FileWriter(Paths.get(AppConfig.HISTORY_FILE).toFile)
    fileWriter.write(f)
    fileWriter.flush()
    fileWriter.close()
  }
}
