package com.github.chengpohi

import java.io.FileWriter
import java.nio.file.Paths

import com.github.chengpohi.config.AppConfig
import com.github.chengpohi.file.{FileTable, PathReader}
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
trait SyncScheduler extends Runnable {
  lazy val LOGER = LoggerFactory.getLogger(getClass.getName)
  val fileTable: FileTable
  val pathReader: PathReader
  override def run(): Unit = {
    LOGER.info("Start Updating File")
    val ft = FileTable.apply(pathReader)
    val deleteFiles = fileTable.records.diff(ft.records)
    LOGER.info("Delete Files: " + deleteFiles)
    val newFiles = ft.records.diff(fileTable.records)
    LOGER.info("New Files: " + newFiles)
    fileTable.records = ft.records
    LOGER.info("New FileTable: " + fileTable.records)
  }
}

object SyncScheduler {
  implicit val formats = org.json4s.DefaultFormats
  def apply(pr: PathReader): SyncScheduler = {
    val ft: FileTable = FileTable.apply(pr)
    new SyncScheduler {
      override val pathReader: PathReader = pr
      override val fileTable: FileTable = ft
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          val f = write(fileTable.records)
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
