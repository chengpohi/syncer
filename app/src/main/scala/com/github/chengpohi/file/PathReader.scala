package com.github.chengpohi.file

import java.io.{File, FileNotFoundException}
import java.nio.file.Paths

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.model.Record
import org.json4s._
import org.json4s.native.JsonMethods._

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
    file.listFiles.toList.flatMap(f => rec(file))
  }
}

object PathReader {
  def apply(p: String): PathReader = new PathReader {
    override val path: String = p
  }
}

trait FileTable {
  var records: List[Record]
}

object FileTable {
  implicit val formats = org.json4s.DefaultFormats
  def apply(p: PathReader): FileTable = {
    val rs: List[Record] = p.ls.map(i => Record(i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""),
      md5Hash(i.getAbsolutePath.replaceAll(s"^${AppConfig.SYNC_PATH}", ""))))
    new FileTable {
      override var records: List[Record] = rs
    }
  }

  def readNativeRecords: List[Record] = {
    val io = IO {
      scala.io.Source.fromFile(AppConfig.RECORD_FILE).mkString
    }
    try {
      io.map(raw => parse(raw).extract[List[Record]]).unsafePerformIO()
    } catch {
      case e: FileNotFoundException => List()
    }
  }

  def md5Hash(source: String): String =
    java.security.MessageDigest.getInstance("MD5").digest(source.getBytes).map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
}


