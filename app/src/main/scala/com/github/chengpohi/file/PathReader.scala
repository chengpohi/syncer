package com.github.chengpohi.file

import java.io.{File, FileInputStream}
import java.nio.file.{Path, Paths}
import java.util.Date

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.model.FileItem
import org.json4s._

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
trait PathReader {
  val path: String
  lazy val file: File = Paths.get(path).toFile
  def ls(filter: File => Boolean): List[File] = {
    def rec(file: File): List[File] = file.isDirectory match {
      case true => file.listFiles().flatMap(f => rec(f)).toList
      case false => List(file)
    }
    rec(file).filter(filter)
  }
}

object PathReader {
  def apply(p: String): PathReader = new PathReader {
    override val path: String = p
  }
}

trait Operation

case object Delete extends Operation
case object Create extends Operation

case object OperationSerializer extends CustomSerializer[Operation](format => ( {
  case JString(operation) => operation match {
    case "Delete" => Delete
    case "Create" => Create
  }
  case JNull => null
}, {
  case operation: Operation => JString(operation.getClass.getSimpleName.replace("$", ""))
}))

case class Commit(date: Date, op: Operation, fileItem: FileItem)

case class Diff(local: List[Commit], remote: List[Commit])

case class Repository(fileItems: List[FileItem], commits: List[Commit])

case class FileTable(var existFileItems: List[FileItem], var deletedRecords: List[FileItem], var newRecords: List[FileItem]) {
  def update(et: List[FileItem], dr: List[FileItem], nr: List[FileItem]) = {
    existFileItems = et
    deletedRecords = dr
    newRecords = nr
  }
}

object FileTable {
  implicit val formats = org.json4s.DefaultFormats
  def apply(p: PathReader): FileTable = scanPath(p)(syncFileFilter)

  def scanSendedFiles(p: PathReader): List[FileItem] = {
    p.ls(sendedFiles).map(i =>
      FileItem(
        i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""), checkSum(i))
    )
  }

  def scanPath(p: PathReader)(f: (File) => Boolean): FileTable = {
    val rs: List[FileItem] = p.ls(f).map(i =>
      FileItem(
        i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""), checkSum(i))
    )
    FileTable(rs, List(), List())
  }

  def sendedFiles(file: File) = file.getName.endsWith(".sending")

  def syncFileFilter(file: File) = file match {
    case f: File if f.getName.endsWith(".sending") => false
    case f: File if f.getName.endsWith(".history") => false
    case _ => true
  }

  def checkSum(file: File): String = {
    file.canRead match {
      case true =>
        val length = file.length()
        val inputStream: FileInputStream = new FileInputStream(file)
        val take: Array[Byte] = Stream.continually(inputStream.read).take(8192 * 2).map(_.toByte).toArray
        md5Hash(take :+ length.toByte)
      case false =>
        md5Hash(System.currentTimeMillis().toString)
    }
  }

  def md5Hash(source: String): String = md5Hash(source.getBytes)
  def md5Hash(bytes: Array[Byte]): String =
    java.security.MessageDigest.getInstance("MD5").digest(bytes).map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
  def randomHashPath: Path = {
    initSyncFolder()
    Paths.get(AppConfig.SYNC_HISTORY_FOLDER + "/" + md5Hash(System.currentTimeMillis().toString) + ".sending")
  }

  def initSyncFolder() = {
    val file: File = new File(AppConfig.SYNC_HISTORY_FOLDER)
    file.exists() match {
      case true =>
      case false => file.mkdir()
    }
  }
}

class FileTableDAO(pathReader: PathReader) {
  def getFiles = FileTable.apply(pathReader).existFileItems
  def getCopiedFile(checkSum: String) = {
    val files: List[FileItem] = FileTable.scanSendedFiles(pathReader)
    files.filter(f => f.md5 == checkSum).head
  }
  val sendFiles = (file: File) => file.getName.endsWith(".sending")
}

object FileTableDAO {
  def apply(pathReader: PathReader): FileTableDAO = new FileTableDAO(pathReader)
}
