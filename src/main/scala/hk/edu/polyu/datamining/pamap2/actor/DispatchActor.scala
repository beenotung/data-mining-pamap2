package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable

/**
  * Created by beenotung on 2/18/16.
  */

class DispatchActor extends Actor with ActorLogging {
  val workers = mutable.Map.empty[ActorRef, String]
  val nodeInfos = mutable.Map.empty[String, NodeInfo]

  override def receive: Receive = {
    case nodeInfo: NodeInfo => nodeInfos += ((nodeInfo.clusterSeedId, nodeInfo))
    case MessageProtocol.RegisterWorker(clusterSeedId) => workers += ((sender(), clusterSeedId))
    case MessageProtocol.UnRegisterWorker(clusterSeedId) => workers -= sender()
    case msg: MessageProtocol.Request[MessageProtocol.ClusterComputeInfo] => sender() ! nodeInfos.values.toIndexedSeq
    case msg => log error s"Unsupported msg : $msg"
      ???
  }
}
