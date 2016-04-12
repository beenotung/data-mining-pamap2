package hk.edu.polyu.datamining.pamap2.actor

import java.{util => ju}

import akka.actor.ActorSystem
import com.rethinkdb.RethinkDB.r
import com.rethinkdb.model.MapObject
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.{NodeInfo, Task}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.Task.{Label, Offset, TaskType}
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}

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
    val TaskType = Tables.Task.Field.taskType.toString
    val Label = "label"
    val Offset = "offset"
  }

  sealed trait Task extends Comparable[Task] {
    val actionState: ActionStatus.ActionStatusType
    /** id : volatile **/
    var id: String = null
    val param: MapObject = r.hashMap()

    override def compareTo(o: Task) = id.compareTo(o.id)

    def fromMap(map: ju.Map[String, AnyRef]): Task

    protected def baseMap = r.hashMap()
      .`with`(Task.Param, param)
      .`with`(Task.TaskType, actionState.toString)

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

  case class TaskAssign(taskId: String, task: Task)

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
  }

  val TrainingDataCount = "trainingDataCount"

  case object ImuSomTrainingTask extends Enumeration {
    type LabelType = Value
    val a16, a6, r, m = Value
    //val polar = Value

    def fromMap(map: ju.Map[String, AnyRef]): ImuSomTrainingTask = new ImuSomTrainingTask(
      label = withName(map.get(Label).asInstanceOf[String]),
      trainingDataCount = map.get(TrainingDataCount).asInstanceOf[Long]
    )
  }

  case class ImuSomTrainingTask(label: ImuSomTrainingTask.LabelType, trainingDataCount: Long) extends SomTask {
    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)
      .`with`(Label, label.toString)
      .`with`(TrainingDataCount, trainingDataCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = ImuSomTrainingTask.fromMap(map)

    override val param: MapObject = r.hashMap()
      .`with`(Label, label.toString)
  }

  case object TemperatureSomTrainingTask {
    def fromMap(map: ju.Map[String, AnyRef]): Task = TemperatureSomTrainingTask(map.get(TrainingDataCount).asInstanceOf[Long])
  }

  case class TemperatureSomTrainingTask(trainingDataCount: Long) extends SomTask {
    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)
      .`with`(TrainingDataCount, trainingDataCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = TemperatureSomTrainingTask.fromMap(map)

    override val param: MapObject = r.hashMap()
      .`with`(Label, Tables.IMU.Field.temperature.toString)
  }

  case object HeartRateSomTrainingTask {
    def fromMap(map: ju.Map[String, AnyRef]): Task = HeartRateSomTrainingTask(map.get(TrainingDataCount).asInstanceOf[Long])
  }

  case class HeartRateSomTrainingTask(trainingDataCount: Long) extends SomTask {
    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)
      .`with`(TrainingDataCount, trainingDataCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = HeartRateSomTrainingTask.fromMap(map)

    override val param: MapObject = r.hashMap()
      .`with`(Label, Tables.RawData.Field.heartRate.toString)
  }

  case object WeightSomTrainingTask {
    def fromMap(map: ju.Map[String, AnyRef]) = WeightSomTrainingTask()
  }

  case class WeightSomTrainingTask() extends SomTask {
    override def fromMap(map: ju.Map[String, AnyRef]): Task = WeightSomTrainingTask.fromMap(map)

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)

    override val param: MapObject = r.hashMap()
      .`with`(Label, Tables.Subject.Field.weight.toString)
  }

  case object HeightSomTrainingTask {
    def fromMap(map: ju.Map[String, AnyRef]) = HeightSomTrainingTask()
  }

  case class HeightSomTrainingTask() extends SomTask {
    override def fromMap(map: ju.Map[String, AnyRef]): Task = HeightSomTrainingTask.fromMap(map)

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)

    override val param: MapObject = r.hashMap()
      .`with`(Label, Tables.Subject.Field.height.toString)
  }

  case object AgeSomTrainingTask {
    def fromMap(map: ju.Map[String, AnyRef]) = AgeSomTrainingTask()
  }

  case class AgeSomTrainingTask() extends SomTask {
    override def fromMap(map: ju.Map[String, AnyRef]): Task = AgeSomTrainingTask.fromMap(map)

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)

    override val param: MapObject = r.hashMap()
      .`with`(Label, Tables.Subject.Field.age.toString)
  }

  object MapRawDataToItemTask {
    val IMUIds = "imuIds"

    def fromMap(map: ju.Map[String, AnyRef]): MapRawDataToItemTask = new MapRawDataToItemTask(
      map.get(IMUIds).asInstanceOf[String],
      map.get(Offset).asInstanceOf[Long]
    )
  }

  case class MapRawDataToItemTask(imuIds: String, offset: Long) extends Task {
    override val actionState: ActionStatusType = ActionStatus.mapRawDataToItem

    override def toMap: MapObject = r.hashMap(Task.Param, param)
      .`with`(TaskType, actionState.toString)
      .`with`(MapRawDataToItemTask.IMUIds, imuIds)
      .`with`(Offset, offset)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = MapRawDataToItemTask.fromMap(map)
  }

  case object FirstSequenceGenerationTask {
    val ActivityOffset = "activity_offset"

    def fromMap(map: ju.Map[String, AnyRef]): Task = new FirstSequenceGenerationTask(
      map.get(ActivityOffset).asInstanceOf[Long]
    )
  }

  case class FirstSequenceGenerationTask(activityOffset: Long) extends Task {
    override val actionState: ActionStatusType = ActionStatus.firstSequenceGeneration

    override def toMap: MapObject = baseMap
      .`with`(FirstSequenceGenerationTask.ActivityOffset, activityOffset)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = FirstSequenceGenerationTask.fromMap(map)
  }

  object SequenceGenerationTask {
    val ActivityOffset = "activity_offset"
    val SeqOffset = "seq_offset"
    val SeqCount = "seq_count"

    def fromMap(map: ju.Map[String, AnyRef]): Task = new SequenceGenerationTask(
      map.get(ActivityOffset).asInstanceOf[Long],
      map.get(SeqOffset).asInstanceOf[Long],
      map.get(SeqCount).asInstanceOf[Long]
    )
  }

  case class SequenceGenerationTask(activityOffset: Long, seqOffset: Long, seqCount: Long) extends Task {
    override val actionState: ActionStatusType = ActionStatus.sequenceGeneration

    import SequenceGenerationTask._

    override def toMap: MapObject = baseMap
      .`with`(ActivityOffset, activityOffset)
      .`with`(SeqOffset, seqOffset)
      .`with`(SeqCount, seqCount)

    override def fromMap(map: ju.Map[String, AnyRef]): Task = SequenceGenerationTask.fromMap(map)
  }

  //  case class FirstSequenceReductionTask extends Task
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
