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
  val nodeInfos = mutable.Map.empty[String, NodeInfo]
  val pendingTask = mutable.ListBuffer.empty[Task]

  override def preStart(): Unit = {
    log info "starting Task-Dispatcher"
    //log info s"the path is ${self.path}"
  }

  override def postStop() = {
    log info "stopping dispatcher"
    workers.keys.foreach(r => r ! MessageProtocol.ReBindDispatcher)
  }

  override def receive: Receive = {
    case nodeInfo: NodeInfo => if (nodeInfo.clusterSeedId != null) nodeInfos.put(nodeInfo.clusterSeedId, nodeInfo)
    case RegisterWorker(clusterSeedId, workerId) => workers += ((sender(), new WorkerRecord(clusterSeedId, workerId)))
    case UnRegisterWorker(clusterSeedId) => workers.retain((ref, record) => ref.equals(sender()))
      log warning "removed worker"
    case UnRegisterComputeNode(clusterSeedId) => unregisterComputeNode(clusterSeedId)
      log warning "removed compute node"
    case RequestClusterComputeInfo => sender() ! mkClusterComputeInfo()
    //case MessageProtocol.ExtractFromRaw => extractFromRaw()
    case task: MessageProtocol.Task => dispatch(task)
    //case msg => log error s"Unsupported msg : $msg"
  }

  def unregisterComputeNode(clusterSeedId: String) = {
    DatabaseHelper.removeSeed(clusterSeedId)
    nodeInfos.remove(clusterSeedId)
    //TODO to test
    val workerIds = workers.values.filter(clusterSeedId.equals).map(_.workerId).toIndexedSeq
    unregisterWorkers(workerIds)
  }

  def unregisterWorkers(workerIds: IndexedSeq[String]) = {
    val tasks: Seq[Task] = workerIds.flatMap(workerId => DatabaseHelper.getTasksByWorkerId(workerId))
    workers.retain((ref, record) => !workerIds.contains(record.workerId))
    tasks.foreach(task => dispatch(task, reassign = true))
  }

  def dispatch(task: Task, reassign: Boolean = false): Unit = {
    if (workers.isEmpty) {
      log warning "recevied task, but no worker"
      pendingTask += task
    } else {
      // pick the less busy worker (min. pending task)
      val worker = workers.minBy(_._2.pendingTask)
      if (reassign) {
        // reset create time and worker id
        DatabaseHelper.reassignTask(task.id, worker._2.clusterSeedId, worker._2.workerId)
      } else {
        // stamp the task
        task.id = DatabaseHelper.addNewTask(task, worker._2.clusterSeedId, worker._2.workerId)
      }
      worker._1 ! task
      worker._2.pendingTask += 1
    }
  }

  def mkClusterComputeInfo(): ClusterComputeInfo = {
    val margin = getMargin
    ClusterComputeInfo(
      workers.values.filterNot(_.clusterSeedId == null)
        .groupBy(_.clusterSeedId)
        .flatMap(workerGroup => {
          nodeInfos.get(workerGroup._1) match {
            case None =>
              log info s"skip this node : ${workerGroup._1}"
              log info s"all nodes $nodeInfos"
              None
            case Some(node) => Some(ComputeNodeInfo(node, workerGroup._2.toIndexedSeq))
          }
        })
        .toIndexedSeq
    )
  }

  /* remove dead Compute Nodes */
  def checkComputeNodes(): Unit = {
    val margin = getMargin
    val outdatedIds = nodeInfos.values.filter(_.genTime < margin).map(_.clusterSeedId).toIndexedSeq
    workers.retain((_, record) => outdatedIds.contains(record.clusterSeedId))
    outdatedIds.foreach(nodeInfos.remove)
    outdatedIds.foreach(id => self ! UnRegisterComputeNode(id))
  }

  def getMargin: Long = System.currentTimeMillis - Main.config.getLong("clustering.report.timeout")

  @deprecated
  def extractFromRaw(): Unit = {
    DatabaseHelper.getRawDataFileIds().asScala.grouped(workers.size).foreach(ids => dispatch(new ExtractFromRaw(ids.toIndexedSeq)))
  }
}
