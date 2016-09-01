package com.github.chengpohi.file

import java.io.File
import java.nio.file.Paths
import java.util.Date

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.model.FileItem
import org.json4s._

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
trait PathReader {
  val RECORD_FILE = "record.json"
  val path: String
  lazy val file: File = Paths.get(path).toFile
  def ls(filter: File => Boolean): List[File] = {
    def rec(file: File): List[File] = file.isDirectory match {
      case true => file.listFiles().flatMap(f => rec(f)).toList
      case false => List(file)
    }
    file.listFiles.toList.flatMap(f => rec(file)).filter(!_.getName.endsWith(".sending"))
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
  def apply(p: PathReader): FileTable = scanPath(p)

  def scanPath(p: PathReader): FileTable = {
    val rs: List[FileItem] = p.ls(ignoreSending).map(i =>
      FileItem(
        i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""),
        md5Hash(i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}",
          ""))
      )
    )
    FileTable(rs, List(), List())
  }

  val ignoreSending = (file: File) => file.getName.endsWith(".sending")

  def md5Hash(source: String): String =
    java.security.MessageDigest.getInstance("MD5").digest(source.getBytes).map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
}

class FileTableDAO(pathReader: PathReader) {
  def getFiles = FileTable.apply(pathReader).existFileItems
}

object FileTableDAO {
  def apply(pathReader: PathReader): FileTableDAO = new FileTableDAO(pathReader)
}
