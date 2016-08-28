package com.github.chengpohi

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.github.chengpohi.model.Record

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
case class DeleteFile(rs: List[Record])
case class NewFile(rs: List[Record])

class SyncActor extends Actor with ActorLogging{
  val selection: ActorSelection = context.actorSelection(s"akka.tcp://Syncer@127.0.0.1:8888/user/syncer")
  override def receive: Receive = {
    case d: DeleteFile => {
      log.info(s"delete files: ${d.rs.flatMap(f => f.name)}")
    }
    case n: NewFile => {
      log.info(s"delete files: ${n.rs.flatMap(f => f.name)}")
    }
  }
}
