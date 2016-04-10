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

  object Task {
    val Param = "param"
  }

  sealed trait Task extends Comparable[Task] {
    val actionState: ActionStatus.ActionStatusType
    var id: String = null
    val param: MapObject = r.hashMap()

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

  case object DispatcherHeartBeat

  case object ComputeNodeHeartBeat

  //  @deprecated("meet bottleneck at database, holding too much data in ram")
  //  case class ExtractFromRaw(ids: Seq[String]) extends Task

  //  @deprecated("meet bottleneck at database, holding too much data in ram")
  //  case class ProcessRawLines(filename: String, lines: ju.List[String], fileType: ImportActor.FileType.FileType) extends Task

  case class ResponseClusterComputeInfo(clusterComputeInfo: ClusterComputeInfo)

  case object ReBindDispatcher

  case object RequestClusterComputeInfo extends Request

  case object RequestNodeInfo extends Request

  case class StartARM(percentage: Double, start: Double, end: Double, step: Double)

  abstract class SomTask extends Task {
    override val actionState: ActionStatusType = ActionStatus.somProcess
    val Label = "label"
  }

  case object ImuSomTrainingTask extends Enumeration {
    val Label = "label"
    val TrainingDataCount = "trainingDataCount"

    type LabelType = Value
    val a16, a6, r, m, polar = Value

    def fromMap(map: ju.Map[String, AnyRef]): ImuSomTrainingTask = new ImuSomTrainingTask(
      label = withName(map.get(Label).asInstanceOf),
      trainingDataCount = map.get(TrainingDataCount).asInstanceOf
    )
  }

  case class ImuSomTrainingTask(label: ImuSomTrainingTask.LabelType, trainingDataCount: Long) extends SomTask {
    override val actionState: ActionStatusType = ActionStatus.somProcess

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(ImuSomTrainingTask.Label, label.toString)
      .`with`(ImuSomTrainingTask.TrainingDataCount, trainingDataCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = ImuSomTrainingTask.fromMap(map)

    override val param: MapObject = r.hashMap()
      .`with`(Label, ImuSomTrainingTask.getClass.toString)
  }

  case object TemperatureSomTrainingTask {
    val TrainingDataCount = ImuSomTrainingTask.TrainingDataCount

    def fromMap(map: ju.Map[String, AnyRef]): Task = TemperatureSomTrainingTask(map.get(TrainingDataCount).asInstanceOf)
  }

  case class TemperatureSomTrainingTask(trainingDataCount: Long) extends SomTask {
    override val actionState: ActionStatusType = ActionStatus.somProcess

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TemperatureSomTrainingTask.TrainingDataCount, trainingDataCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = TemperatureSomTrainingTask.fromMap(map)

    override val param: MapObject = r.hashMap()
      .`with`(Label, TemperatureSomTrainingTask.getClass.toString)
  }

  case object HeartRateSomTrainingTask {
    val TrainingDataCount = ImuSomTrainingTask.TrainingDataCount

    def fromMap(map: ju.Map[String, AnyRef]): Task = HeartRateSomTrainingTask(map.get(TrainingDataCount).asInstanceOf)
  }

  case class HeartRateSomTrainingTask(trainingDataCount: Long) extends SomTask {
    override val actionState: ActionStatusType = ActionStatus.somProcess

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(ImuSomTrainingTask.TrainingDataCount, trainingDataCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = HeartRateSomTrainingTask.fromMap(map)

    override val param: MapObject = r.hashMap()
      .`with`(Label, HeartRateSomTrainingTask.getClass.toString)
  }

  object ItemCountTask {
    val IMUIds = "imuIds"
    val Offset = "offset"

    def fromMap(map: ju.Map[String, AnyRef]): ItemCountTask = new ItemCountTask(
      map.get(IMUIds).asInstanceOf,
      map.get(Offset).asInstanceOf
    )
  }

  case class ItemCountTask(imuIds: String, offset: Long) extends Task {
    override val actionState: ActionStatusType = ActionStatus.itemCount

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(ItemCountTask.IMUIds, imuIds)
      .`with`(ItemCountTask.Offset, offset)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = ItemCountTask.fromMap(map)
  }

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
