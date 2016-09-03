package com.github.chengpohi.tcp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Sink, Tcp}
import akka.util.ByteString
import com.github.chengpohi.file.FileTable
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
  * syncer
  * Created by chengpohi on 8/31/16.
  */
class Receiver(system: ActorSystem, address: String, port: Int) {
  lazy val log = LoggerFactory.getLogger(getClass.getName)
  implicit val sys = system

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
    log.info("Connect from: {}", conn.remoteAddress)
    conn handleWith Flow[ByteString].alsoTo(FileIO.toPath(FileTable.randomHashPath)).map(i => ByteString.empty)
  }

  val connections = Tcp().bind(address, port)
  val binding = connections.to(handler).run()

  binding.onComplete {
    case Success(b) =>
      log.info("Receiver started, listen on: {}", b.localAddress)
    case Failure(e) =>
      log.info("Server bind failed, address: {} port: {} errorMsg: {}", address, port.toString, e.getMessage)
      system.terminate()
  }
}

object Receiver {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("Syncer", ConfigFactory.load())
    new Receiver(actorSystem, "localhost", 8888)
  }
}
