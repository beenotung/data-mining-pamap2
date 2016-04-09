package hk.edu.polyu.datamining.pamap2.actor

import java.{util => ju}

import akka.actor.ActorSystem
import com.rethinkdb.RethinkDB.r
import com.rethinkdb.model.MapObject
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.NodeInfo
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 2/18/16.
  */
object MessageProtocol {

  type ClusterComputeInfo = IndexedSeq[ComputeNodeInfo]

  sealed trait Request

  sealed trait Response

  sealed trait DispatchActorProtocol

  sealed trait Task extends Comparable[Task] {
    val actionState: ActionStatus.ActionStatusType
    var id: String = null

    override def compareTo(o: Task) = id.compareTo(o.id)

    def fromMap(map: ju.Map[String, AnyRef]): Task

    def toMap: MapObject
  }

  case class NodeInfo(processor: Int, freeMemory: Long, totalMemory: Long, maxMemory: Long, upTime: Long, startTime: Long, clusterSeedId: String, genTime: Long) extends Comparable[NodeInfo] {
    override def compareTo(o: NodeInfo): Int = clusterSeedId.compareTo(o.clusterSeedId)
  }

  case class WorkerRecord(clusterSeedId: String, workerId: String, var pendingTask: Long, var completedTask: Long)

  case class ComputeNodeInfo(nodeInfo: NodeInfo, workerRecords: Seq[WorkerRecord])

  case class RegisterWorker(clusterSeedId: String, workerId: String) extends DispatchActorProtocol

  case class UnRegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  case class UnRegisterComputeNode(clusterSeedId: String) extends DispatchActorProtocol

  case class TaskCompleted(taskId: String)

  //  @deprecated("meet bottleneck at database, holding too much data in ram")
  //  case class ExtractFromRaw(ids: Seq[String]) extends Task

  //  @deprecated("meet bottleneck at database, holding too much data in ram")
  //  case class ProcessRawLines(filename: String, lines: ju.List[String], fileType: ImportActor.FileType.FileType) extends Task

  case class ResponseClusterComputeInfo(clusterComputeInfo: ClusterComputeInfo)

  case object ReBindDispatcher

  case object RequestClusterComputeInfo extends Request

  case object RequestNodeInfo extends Request

  case class StartARM(percentage: Double, start: Double, end: Double, step: Double)

  object SOMProcessTask {
    val actionState: ActionStatusType = ActionStatus.somProcess
    val BodyPart = "bodyPart"
    val Count = "count"

    def fromMap(map: ju.Map[String, AnyRef]): Task = new SOMProcessTask(
      map.get(BodyPart).asInstanceOf,
      map.get(Count).asInstanceOf
    )
  }

  /** @param bodyPart : hand | ankle | chest **/
  case class SOMProcessTask(bodyPart: String, count: Long) extends Task {
    override val actionState: ActionStatusType = SOMProcessTask.actionState

    override def fromMap(map: ju.Map[String, AnyRef]): Task = SOMProcessTask.fromMap(map)

    override def toMap: MapObject = r.hashMap(SOMProcessTask.BodyPart, bodyPart)
  }

  //  case class ItemCountTask(field:String) extends Task{
  //    override val actionStatus:ActionStatusType=ActionStatus.itemCount
  //  }

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
        },
        genTime = System.currentTimeMillis()
      )
    }
  }

}
