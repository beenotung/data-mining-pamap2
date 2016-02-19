package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.ActorSystem
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.NodeInfo
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 2/18/16.
  */
object MessageProtocol {

  sealed trait Request

  sealed trait DispatchActorProtocol

  sealed trait Task

  case class NodeInfo(val processor: Int, val freeMemory: Long, val totalMemory: Long, val maxMemory: Long, val upTime: Long, val startTime: Long, val clusterSeedId: String) extends Comparable[NodeInfo] {
    override def compareTo(o: NodeInfo): Int = clusterSeedId.compareTo(o.clusterSeedId)
  }

  case class WorkerRecord(val clusterSeedId: String, var pendingTask: Int = 0, var completedTask: Int = 0)

  case class ComputeNodeInfo(val nodeInfo: NodeInfo, val workerRecords: Seq[WorkerRecord])

  case class ClusterComputeInfo(nodeInfo: Seq[ComputeNodeInfo])

  case class RegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  case class UnRegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  case class ExtractFromRaw(ids: Seq[String]) extends Task

  case object RequestClusterComputeInfo extends Request

  case object RequestNodeInfo extends Request

}

object MessageProtocolFactory {

  object NodeInfo {
    def newInstance(implicit system: ActorSystem): NodeInfo = {
      val runtime = Runtime.getRuntime
      new NodeInfo(
        processor = runtime.availableProcessors(),
        freeMemory = runtime.freeMemory(),
        totalMemory = runtime.totalMemory(),
        maxMemory = runtime.maxMemory(),
        upTime = system.uptime,
        startTime = system.startTime,
        clusterSeedId = {
          val id = DatabaseHelper.clusterSeedId
          if (id == null) println(s"warning : clusterSeedId is null !!!")
          id
        }
      )
    }
  }

}
