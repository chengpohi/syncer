package com.github.chengpohi.repository

import java.io.{File, FileNotFoundException, FileWriter}
import java.nio.file.{Files, Path, Paths}
import java.util.Date
import java.util.concurrent.locks.ReentrantLock

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file
import com.github.chengpohi.file.{Commit, Create, Delete, FileTableDAO, OperationSerializer, Repository}
import com.github.chengpohi.model.FileItem
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory

import scalaz.Scalaz._
import scalaz.effect.IO

/**
  * syncer
  * Created by chengpohi on 8/30/16.
  */
class RepositoryService(fileTableDAO: FileTableDAO) {
  lazy val log = LoggerFactory.getLogger(getClass.getName)

  import com.github.chengpohi.model.FileItemOps._

  implicit val formats = org.json4s.DefaultFormats + OperationSerializer
  private val lock = new ReentrantLock()
  def commit() = {
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
        log.info("new files: {}", newFiles)
        log.info("before files: {}", rep.fileItems)
        val deleteCommits: List[Commit] = deleteFiles.map(f => Commit(new Date, Delete, f))
        val createCommits: List[Commit] = newFiles.map(f => Commit(new Date, Create, f))
        Repository(fileItems, rep.commits ::: deleteCommits ::: createCommits)
      }
    }
    writeRepository(updatedRepository)
    updatedRepository
  }

  def diff(requestRepository: Repository): file.Diff = {
    readRepository match {
      case Some(localRepo) =>
        val remoteDiffCommits: List[Commit] = localRepo.commits.diff(requestRepository.commits)
        val localDiffCommits: List[Commit] = requestRepository.commits.diff(localRepo.commits)
        file.Diff(localDiffCommits, remoteDiffCommits)
    }
  }

  def mergeCreateCommit(commit: Commit): Boolean = {
    val repository = readRepository.get
    val updated: Repository = repository.copy(fileItems = repository.fileItems :+ commit.fileItem,
      commits = repository.commits :+ commit)
    writeRepository(updated)
    true
  }

  def mergeDeleteCommit(commit: Commit): Boolean = {
    val repository = readRepository.get
    val updated: Repository = repository.copy(commits = repository.commits :+ commit)
    writeRepository(updated)
    val file: File = commit.fileItem.toFile
    if (file.exists()) {
      file.delete()
    }
    false
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

  def getCopiedPath(fileItem: FileItem) = fileTableDAO.getCopiedFile(fileItem.md5).toPath

  def copy(tmpPath: Path, distPath: Path) = {
    createParentFile(distPath)
    Files.move(tmpPath, distPath)
  }

  def createParentFile(path: Path) = {
    val toFile: File = path.toFile
    toFile.getParentFile.mkdirs()
  }

  def tryLock = lock.tryLock()
  def unlock = lock.unlock()
}
