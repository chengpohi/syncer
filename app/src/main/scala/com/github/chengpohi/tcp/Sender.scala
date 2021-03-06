package com.github.chengpohi.tcp

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Tcp}
import com.github.chengpohi.file.{Commit, Create}
import com.github.chengpohi.model.FileItem
import com.github.chengpohi.service.SendFileDone
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
  * syncer
  * Created by chengpohi on 8/31/16.
  */
class Sender(system: ActorSystem, address: String, port: Int, receiver: ActorRef, c: Commit) {
  import com.github.chengpohi.model.FileItemOps._
  lazy val log = LoggerFactory.getLogger(getClass.getName)
  implicit val sys = system

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  def send(dist: String) = {
    FileIO.fromPath(c.fileItem.toPath).via(Tcp().outgoingConnection(address, port))
        .runForeach(i => "").onComplete {
      case Success(result) =>
        if (receiver != null) {
          receiver ! SendFileDone(c)
        }
        log.info("Send Commit finished: {}", c)
      case Failure(e) =>
        log.error("Failure: {}", e.getMessage)
    }
  }

}

object Sender {
  def main(args: Array[String]): Unit = {
    val fileItem = FileItem("syncer.jar", "fasdf")
    val c = Commit(new Date(), Create, fileItem)
    val dist: String = "tt.jar"
    val actorSystem = ActorSystem("Client", ConfigFactory.load())
    new Sender(actorSystem, "localhost", 8888, null, c).send(dist)
  }
}
