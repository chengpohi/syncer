package com.github.chengpohi.file

import java.io.{File, FileNotFoundException}
import java.nio.file.Paths

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.model.Record
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz.Scalaz._
import scalaz.effect.IO

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
trait PathReader {
  val RECORD_FILE = "record.json"
  val path: String
  lazy val file: File = Paths.get(path).toFile
  def ls: List[File] = {
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

case class FileTable(var existRecords: List[Record], var deletedRecords: List[Record], var newRecords: List[Record]) {
  def update(et: List[Record], dr: List[Record], nr: List[Record]) = {
    existRecords = et
    deletedRecords = dr
    newRecords = nr
  }
}

object FileTable {
  implicit val formats = org.json4s.DefaultFormats
  def apply(p: PathReader): FileTable = readLocalRecords match {
    case Some(f) => f
    case None => scanPath(p)
  }

  def scanPath(p: PathReader): FileTable = {
    val rs: List[Record] = p.ls.map(i => Record(i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""),
      md5Hash(i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""))))
    FileTable(rs, List(), List())
  }

  def readLocalRecords: Option[FileTable] = {
    val io = IO {
      scala.io.Source.fromFile(AppConfig.RECORD_FILE).mkString
    }
    try {
      io.map(raw => parse(raw).extract[FileTable]).unsafePerformIO().some
    } catch {
      case e: FileNotFoundException => None
    }
  }

  def md5Hash(source: String): String =
    java.security.MessageDigest.getInstance("MD5").digest(source.getBytes).map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
}

class FileTableDAO(pathReader: PathReader) {
  val fileTable: FileTable = FileTable.apply(pathReader)

  def get = fileTable

  def newest: FileTable = {
    val ft = FileTable.scanPath(pathReader)
    val deleteFiles = fileTable.existRecords.diff(ft.existRecords)
    val newFiles = ft.existRecords.diff(fileTable.existRecords)

    fileTable.update(ft.existRecords,
      (fileTable.deletedRecords ::: deleteFiles).distinct,
      (fileTable.newRecords ::: newFiles).distinct)
    fileTable
  }

  def newFiles(ft: FileTable): List[Record] = {
    ft.newRecords.diff(fileTable.existRecords)
  }

  def deleteFiles(ft: FileTable): List[Record] = {
    ft.deletedRecords
  }
}

object FileTableDAO {
  def apply(pathReader: PathReader): FileTableDAO = new FileTableDAO(pathReader)
}
