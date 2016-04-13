package hk.edu.polyu.datamining.pamap2.actor

import java.util.concurrent.{Callable, FutureTask, TimeUnit}
import java.{util => ju}

import akka.actor.ActorRef
import com.rethinkdb.RethinkDB
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.MaxTask
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, DatabaseHelper_, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import hk.edu.polyu.datamining.pamap2.utils.{Lang, Lang_, Log}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration.Duration

/**
  * Created by beenotung on 2/18/16.
  */
object DispatchActor {
  lazy val MaxTask = Main.config.getInt("algorithm.task.max")

  case class WorkerProfile(actorRef: ActorRef, workerRecord: WorkerRecord)

  case class WorkerAddress(clusterSeedId: String, workerId: String)

}

class DispatchActor extends CommonActor {
  val workers = mutable.Map.empty[ActorRef, WorkerRecord]
  val nodeInfos = mutable.Map.empty[String, NodeInfo]
  //  var currentTypePendingTaskCount: Long = {
  //    import Tables.Task.Field
  //    var currentActionType = DatabaseHelper.getActionStatus
  //    DatabaseHelper.run(r => r.table(Tables.Task.name)
  //      .filter(r.hashMap(Field.taskType.toString, currentActionType.toString))
  //      .filter(reqlFunction1(row => r.not(row.hasFields(Field.completeTime.toString))))
  //      .count()
  //    )
  //  }
  var isDoingRestart = false

  override def preStart(): Unit = {
    log info "starting Task-Dispatcher"
    //log info s"the path is ${self.path}"
    //    system.scheduler.schedule(initialDelay = Duration.Zero,
    //      interval = Duration(DispatchActor.ReportInterval, TimeUnit.MILLISECONDS),
    //      receiver = self, message = DispatcherHeartBeat)
    //    import Tables.Task.{Field => fs}
    //    DatabaseHelper.run(r => r.table(Tables.Task.name).filter())
    //    DatabaseHelper.getTasksByWorkerId(workerId = )
  }

  override def postStop() = {
    log info "stopping dispatcher"
    workers.keys.foreach(r => r ! MessageProtocol.ReBindDispatcher)
    if (isDoingRestart)
      Main.mainRun()
  }

  override def receive: Receive = {
    case RestartCluster =>
      isDoingRestart = true
      workers.foreach(_._1 ! RestartCluster)
      system.terminate()
    case nodeInfo: NodeInfo => if (nodeInfo.clusterSeedId != null) nodeInfos.put(nodeInfo.clusterSeedId, nodeInfo)
      sender() ! DispatcherHeartBeat
    case RegisterWorker(clusterSeedId, workerId) =>
      val numberOfPendingTask: Long = DatabaseHelper.run(r => r.table(Tables.Task.name)
        .filter(r.hashMap(Tables.Task.Field.workerId.toString, workerId))
        .filter(r.hashMap(Tables.Task.Field.pending.toString, true))
        .count()
      )
      workers += ((sender(), new WorkerRecord(clusterSeedId, workerId, numberOfPendingTask, 0)))
      Log.info(s"register worker $workerId")
      cleanTasks()
    //TODO get worker record from database
    case UnRegisterWorker(clusterSeedId) => workers.retain((ref, record) => ref.equals(sender()))
      log warning "removed worker"
    case UnRegisterComputeNode(clusterSeedId) => unregisterComputeNode(clusterSeedId)
      log warning "removed compute node"
    case RequestClusterComputeInfo => sender() ! ResponseClusterComputeInfo(mkClusterComputeInfo)
    //      log info s"responsed cluster compute info, sender:$sender"
    case StartARM(percentage, start, end, step) =>
      //TODO working here
      /*
      * 1. label training data using the percentage
      * 2. fire som training on labeled data
      * 3. fire item extract on labeled data
      * 4. fire item count
      * 5. fire confidence, interest counting
      * */
      DatabaseHelper.setActionStatus(ActionStatus.sampling)
      /* step 1. */
      Log.info(s"start mark train sample (${percentage * 100d}%)")
      fork(() => {
        val count: Long = DatabaseHelper.markTrainSample(percentage)
        Log.info("finished mark train sample")
        if (count == 0)
          Log info "Warning no training sample is selected"
        else
          self ! function(() => {
            /* step 2. */
            Log.info(s"start som process (count:$count)")
            DatabaseHelper.setActionStatus(ActionStatus.somProcess)
            findAndDispatchNewTasks(ActionStatus.somProcess, Map(MessageProtocol.TrainingDataCount -> count))
            /* step 3,4,5 (in TaskCompleted) */
          })
      })
    case fun: Lang_.Function => fun.apply()
    case task: MessageProtocol.Task => handleTask(Seq(task))
    case TaskCompleted(taskId) =>
      workers.get(sender()) match {
        case Some(worker) =>
          worker.completedTask += 1
          worker.removeTask(taskId)
        case None =>
      }
      //      currentTypePendingTaskCount -= 1
      val currentActionType = DatabaseHelper.getActionStatus
      val currentTypePendingTaskCount = DatabaseHelper.getPendingTaskCount(currentActionType)
      Log info s"finished 1 task of $currentActionType, number of task remaining = $currentTypePendingTaskCount"
      if (currentTypePendingTaskCount == 0)
        onTaskTypeCompleted(currentActionType)
      else
        cleanTasks()
  }

  def onTaskTypeCompleted(currentActionType: ActionStatus.ActionStatusType) =
    currentActionType match {
      case ActionStatus.finished => Log.info("all task finished?")
      case ActionStatus.somProcess =>
        Log.info("finished som training")
        findAndDispatchNewTasks(ActionStatus.mapRawDataToItem)
      case ActionStatus.mapRawDataToItem =>
        Log.info(s"finished raw data to item mapping")
        findAndDispatchNewTasks(ActionStatus.firstSequenceGeneration)
      case ActionStatus.firstSequenceGeneration =>
        Log info "finished first sequence generation"
        findAndDispatchNewTasks(ActionStatus.firstSequenceReduction)
      case ActionStatus.firstSequenceReduction =>
        Log info "finished first sequence reduction"
        DatabaseHelper.setArmLNum(2)
        findAndDispatchNewTasks(ActionStatus.sequenceGeneration)
      case ActionStatus.sequenceGeneration =>
        val l = DatabaseHelper.getArmLNum
        Log.info(s"finished sequence generation on L$l")
        findAndDispatchNewTasks(ActionStatus.sequenceReduction)
      case ActionStatus.sequenceReduction =>
        val l = DatabaseHelper.getArmLNum
        Log info s"finished sequence reduction on L$l"
        //TODO detect to stop
        if (true) {
          DatabaseHelper.setArmLNum(l + 1)
          findAndDispatchNewTasks(ActionStatus.sequenceGeneration)
        } else {
          findAndDispatchNewTasks(ActionStatus.ruleGeneration)
        }
      case status: ActionStatus.ActionStatusType =>
        Log info s"finished all task of $status"
        findAndDispatchNewTasks(ActionStatus.next(status))
    }

  def unregisterComputeNode(clusterSeedId: String) = {
    DatabaseHelper.removeSeed(clusterSeedId)
    nodeInfos.remove(clusterSeedId)
    //TODO to test
    val workerIds = workers.values.filter(clusterSeedId.equals).map(_.workerId).toIndexedSeq
    unregisterWorkers(workerIds)
  }

  def unregisterWorkers(workerIds: IndexedSeq[String]) = {
    val tasks: Seq[Task] = workerIds.flatMap(workerId => DatabaseHelper.getActiveTasksByWorkerId(workerId))
    workers.retain((ref, record) => !workerIds.contains(record.workerId))
    handleTask(tasks)
    cleanTasks()
  }

  def mkClusterComputeInfo: IndexedSeq[ComputeNodeInfo] = {
    //    log info "making cluster compute info"
    val margin = getMargin
    val deadWorkers = mutable.Buffer.empty[String]
    val infos = workers.values.filterNot(_.clusterSeedId == null)
      .groupBy(_.clusterSeedId)
      .flatMap(workerGroup => {
        nodeInfos.get(workerGroup._1) match {
          case None =>
            log info s"skip this node : ${workerGroup._1}"
            log info s"all nodes $nodeInfos"
            deadWorkers ++ workerGroup._1
            None
          case Some(node) => Some(ComputeNodeInfo(node, workerGroup._2.toIndexedSeq))
        }
      })
      .toIndexedSeq
    unregisterWorkers(deadWorkers.toIndexedSeq)
    infos
  }

  def findAndDispatchNewTasks(actionState: ActionStatus.ActionStatusType = DatabaseHelper.getActionStatus, param: Map[String, AnyVal] = Map.empty): Unit = {
    val tasks = findNewTasks(actionState, param)
    DatabaseHelper.setActionStatus(actionState)
    //    currentActionType = actionState
    //    currentTypePendingTaskCount = tasks.length
    if (tasks.isEmpty)
      onTaskTypeCompleted(actionState)
    else
      handleTask(tasks)
  }

  def handleTask(tasks: Seq[Task]) = {
    // dispatch or store in pending
    if (workers.isEmpty) {
      Log.info("trying to send task, but no workers")
      addPendingTasks(tasks)
    } else {
      val pendingTasks = mutable.ListBuffer.empty[Task]
      tasks.foreach(task => {
        val (actorRef, record) = workers.minBy(_._2.pendingTask)
        if (record.pendingTask < MaxTask) {
          if (task.id == null) {
            task.id = DatabaseHelper.addNewTask(task, record.clusterSeedId, record.workerId)
          } else {
            DatabaseHelper.reassignTask(task.id, record.clusterSeedId, record.workerId)
          }
          record.addTask(task.id)
          Log.debug(s"sent task $task to $actorRef")
          assert(task.id != null, "task id should not be null")
          actorRef ! TaskAssign(task.id, task)
        } else {
          pendingTasks += task
        }
      })
      addPendingTasks(pendingTasks.toIndexedSeq)
    }
  }

  def cleanTasks() = {
    Log.info("clean tasks")
    if (workers.isEmpty)
      Log.info("no worker, skip cleaning task")
    else {
      val taskQuota: Long = workers.map(x => MaxTask - x._2.pendingTask).sum
      if (taskQuota == 0) {
        Log info s"warming : no taskQuota ${workers.values}"
      }
      else
        handleTask(getPendingTasks(Math.min(numberOfPendingTask, taskQuota)))
    }
  }

  def findNewTasks(actionState: ActionStatus.ActionStatusType = DatabaseHelper.getActionStatus, param: Map[String, Any]): Seq[Task] = {
    Log.debug(s"find new task (${actionState.toString})")
    val xs = actionState match {
      case ActionStatus.somProcess =>
        val existingSoms = DatabaseHelper.runToBuffer[String](r => r.table(Tables.SomImage.name).getField(Tables.SomImage.LabelPrefix)).toSet
        val trainingDataCount: Long = param.get(MessageProtocol.TrainingDataCount).get.asInstanceOf[Long]
        ImuSomTrainingTask.values.toSeq.map(label => new ImuSomTrainingTask(label, trainingDataCount))
          .+:(new TemperatureSomTrainingTask(trainingDataCount))
          .+:(new HeartRateSomTrainingTask(trainingDataCount))
          .+:(new WeightSomTrainingTask)
          .+:(new HeightSomTrainingTask)
          .+:(new AgeSomTrainingTask)
          .filterNot(somTask => existingSoms.contains(somTask.param.get(MessageProtocol.Task.Label).toString))
      case ActionStatus.mapRawDataToItem =>
        val fs = Tables.RawData.Field
        val taskCount: Long = DatabaseHelper.run(r => r.table(Tables.RawData.name)
          .filter(fs.isTrain.toString, true)
          .count()
        )
        val imuIds: String = DatabaseHelper.runToBuffer[String](_.table(Tables.SomImage.name).getField(DatabaseHelper.id))
          .reduce((a, b) => a + b)
        (0L until taskCount).flatMap(offset => Seq(
          new MapRawDataToItemTask(imuIds, offset),
          new MapRawDataToItemTask(imuIds, offset),
          new MapRawDataToItemTask(imuIds, offset)
        ))
      case ActionStatus.firstSequenceGeneration =>
        val activityCount = DatabaseHelper.countTableItem(Tables.ActivityItemSetSequence, RethinkDB.r.hashMap())
        (0L until activityCount).map(activityOffset => new FirstSequenceGenerationTask(activityOffset))
      //      case ActionStatus.firstSequenceReduction =>

      //TODO add more task type
      case _ => Log error s"findTask on $actionState is not implemented"
        ???
    }
    Log.debug(s"number of task found:${xs.length}")
    xs
  }

  def getPendingTasks(limit: Long = -1): Seq[Task] = {
    Log.debug(s"get pending task, limit:$limit")
    val field = Tables.Task.Field
    val result = DatabaseHelper.runToBuffer[ju.Map[String, AnyRef]](r => {
      val query = r.table(Tables.Task.name)
        .filter(r.hashMap(field.pending.toString, true))
      if (limit > 0)
        query.limit(limit)
      else
        query
    }).map(DatabaseHelper.toTask).filterNot(_ == null).toIndexedSeq
    Log.debug(s"get ${result.length} pending task(s)")
    result
  }

  def numberOfPendingTask: Long =
    DatabaseHelper.run(r => r.table(Tables.Task.name).filter(r.hashMap(Tables.Task.Field.pending.toString, true)).count())


  def addPendingTasks(tasks: Seq[Task]): Unit = {
    if (tasks.isEmpty)
      return
    Log.debug(s"added tasks to pending table $tasks")
    /*
    * 1. save new tasks
    * 2. update old tasks
    * */
    val table = Tables.Task.name
    val field = Tables.Task.Field
    val (newTasks, oldTasks) = tasks.partition(_.id == null)
    //println(s"add pending tasks:\nall tasks:$tasks\nnew tasks:$newTasks\nold tasks:$oldTasks")
    /* 1. save new tasks */
    if (newTasks.nonEmpty)
      DatabaseHelper.tableInsertRows(table, newTasks.map(_.toMap.`with`(field.pending.toString, true)).asJava)
    /* 2. update old tasks */
    if (oldTasks.nonEmpty)
      DatabaseHelper.run[ju.HashMap[String, AnyRef]](r => {
        val ids = r.args(oldTasks.map(_.id).asJava)
        val row = r.hashMap(field.pending.toString, true)
        r.table(table).getAll(ids).update(reqlFunction1(x => row))
      })
  }

  /* remove dead Compute Nodes */
  def checkComputeNodes(): Unit = {
    val margin = getMargin
    //TODO add 'removed clusterSeedIds' from DatabaseHelper
    val outdatedIds = nodeInfos.values.filter(_.genTime < margin).map(_.clusterSeedId).toIndexedSeq
    workers.retain((_, record) => outdatedIds.contains(record.clusterSeedId))
    outdatedIds.foreach(nodeInfos.remove)
    outdatedIds.foreach(id => self ! UnRegisterComputeNode(id))
  }

}
