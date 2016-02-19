package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by beenotung on 2/18/16.
  */
class WorkerRecord(actorRef: ActorRef, clusterSeedId: String) {
  var pending: Int = 0
  var completed: Int = 0
}

class DispatchActor extends Actor with ActorLogging {
  val workers = mutable.Map.empty[ActorRef, WorkerRecord]
  val nodeInfos = mutable.Map.empty[String, NodeInfo]

  override def preStart(): Unit = {
    log info "starting Task-Dispatcher"
    log info s"the path is ${self.path}"
  }

  override def receive: Receive = {
    case nodeInfo: NodeInfo => nodeInfos += ((nodeInfo.clusterSeedId, nodeInfo))
    case RegisterWorker(clusterSeedId) => workers += ((sender(), new WorkerRecord(sender(), clusterSeedId)))
    case UnRegisterWorker(clusterSeedId) => workers -= sender()
    case RequestClusterComputeInfo => sender() ! MessageProtocol.ClusterComputeInfo(nodeInfos.values.toIndexedSeq)
    case MessageProtocol.ExtractFromRaw => extractFromRaw()
    case msg => log error s"Unsupported msg : $msg"
      ???
  }

  def extractFromRaw(): Unit = {
    DatabaseHelper.getRawDataFileIds().asScala.grouped(workers.size).foreach(ids => dispatch(new ExtractFromRaw(ids.toIndexedSeq)))
  }

  def dispatch(task: Task): Unit = {
    workers.minBy(_._2.pending)._1 ! task
  }
}
