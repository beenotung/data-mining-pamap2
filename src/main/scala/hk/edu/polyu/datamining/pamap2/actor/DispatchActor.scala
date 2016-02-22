package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Created by beenotung on 2/18/16.
  */
class DispatchActor extends Actor with ActorLogging {
  val workers = mutable.Map.empty[ActorRef, WorkerRecord]
  val nodeInfos = mutable.Map.empty[String, (NodeInfo, Long)]
  val pendingTask = mutable.ListBuffer.empty[Task]

  override def preStart(): Unit = {
    log info "starting Task-Dispatcher"
    //log info s"the path is ${self.path}"
  }

  override def postStop() = {
    log info "stopping dispatcher"
    workers.keys.foreach(r => r ! MessageProtocol.ReBindDispatcher)
  }

  /* remove dead workers */
  def checkWorkers(): Unit = {
    val margin = getMargin
    val outdatedIds = nodeInfos.values.filter(_._2 < margin).map(_._1.clusterSeedId).toIndexedSeq
    workers.retain((_, record) => outdatedIds.contains(record.clusterSeedId))
    nodeInfos --= outdatedIds
  }

  override def receive: Receive = {
    case nodeInfo: NodeInfo => if (nodeInfo.clusterSeedId != null) nodeInfos put(nodeInfo.clusterSeedId, (nodeInfo, System.currentTimeMillis))
    case RegisterWorker(clusterSeedId) => workers += ((sender(), new WorkerRecord(clusterSeedId)))
    case UnRegisterWorker(clusterSeedId) => workers.retain((ref, record) => ref.equals(sender()))
      log warning "removed worker"
    case UnRegisterComputeNode(clusterSeedId) => nodeInfos -= clusterSeedId
      removeWorker(clusterSeedId)
      log warning "removed compute node"
    case RequestClusterComputeInfo => sender() ! mkClusterComputeInfo()
    //case MessageProtocol.ExtractFromRaw => extractFromRaw()
    case task: MessageProtocol.Task => dispatch(task)
    //case msg => log error s"Unsupported msg : $msg"
  }

  def removeWorker(clusterSeedId: String): Unit = workers.retain((ref, record) => !clusterSeedId.equals(record.clusterSeedId))

  def mkClusterComputeInfo(): ClusterComputeInfo = {
    val margin = getMargin
    ClusterComputeInfo(workers.values
      .filterNot(_.clusterSeedId == null)
      .groupBy(_.clusterSeedId)
      .map(workerGroup => ComputeNodeInfo(nodeInfos(workerGroup._1)._1, workerGroup._2.toIndexedSeq))
      .filterNot(x => nodeInfos(x.nodeInfo.clusterSeedId)._2 < margin)
      .toIndexedSeq
    )
  }

  def getMargin: Long = System.currentTimeMillis - Main.config.getLong("clustering.report.timeout")

  def dispatch(task: Task): Unit = {
    if (workers.isEmpty) {
      log warning "recevied task, but no worker"
      pendingTask += task
    } else {
      // pick the less busy worker (min. pending task)
      val worker = workers.minBy(_._2.pendingTask)
      // stamp the task
      task.id = DatabaseHelper.addNewTask(task, worker._2.clusterSeedId)
      worker._1 ! task
      worker._2.pendingTask += 1
    }
  }

  @deprecated
  def extractFromRaw(): Unit = {
    DatabaseHelper.getRawDataFileIds().asScala.grouped(workers.size).foreach(ids => dispatch(new ExtractFromRaw(ids.toIndexedSeq)))
  }
}
