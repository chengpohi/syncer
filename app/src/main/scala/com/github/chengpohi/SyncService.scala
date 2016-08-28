package com.github.chengpohi

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig
import com.github.chengpohi.model.Record

/**
  * syncer
  * Created by chengpohi on 8/28/16.
  */
case class DeleteFile(rs: List[Record])
case class NewFile(rs: List[Record])

class SyncService extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  val workerRouter = context.actorOf(FromConfig.props(Props[SyncWorker]),
    name = "workerRouter")

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case d: DeleteFile => {
      workerRouter.tell(ConsistentHashableEnvelope(d, d), self)
    }
    case n: NewFile => {
      workerRouter.tell(ConsistentHashableEnvelope(n, n), self)
    }
  }
}

class SyncWorker extends Actor with ActorLogging{
  override def receive: Receive = {
     case d: DeleteFile => {
      log.info(s"delete files: ${d.rs.flatMap(f => f.name)}")
    }
    case n: NewFile => {
      log.info(s"new files: ${n.rs.flatMap(f => f.name)}")
    }
  }
}
