package hk.edu.polyu.datamining.pamap2.actor

import java.{util => ju}

import akka.actor.ActorRef
import com.rethinkdb.net.Cursor
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.MaxTask
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}

import scala.collection.JavaConverters._
import scala.collection.mutable

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
    case RegisterWorker(clusterSeedId, workerId) => workers += ((sender(), new WorkerRecord(clusterSeedId, workerId, 0, 0)))
    //TODO get worker record from database
    case UnRegisterWorker(clusterSeedId) => workers.retain((ref, record) => ref.equals(sender()))
      log warning "removed worker"
    case UnRegisterComputeNode(clusterSeedId) => unregisterComputeNode(clusterSeedId)
      log warning "removed compute node"
    case RequestClusterComputeInfo => sender() ! ResponseClusterComputeInfo(mkClusterComputeInfo)
    //      log info s"responsed cluster compute info, sender:$sender"
    case StartARM(start,end,step) =>
      DatabaseHelper.setActionStatus(ActionStatus.preProcess)
      findAndDispatchNewTasks(ActionStatus.preProcess)
      //TODO fire item count
    case task: MessageProtocol.Task => handleTask(Seq(task))
    case TaskCompleted(taskId) => cleanTasks()
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
    handleTask(tasks)
  }

  def mkClusterComputeInfo: ClusterComputeInfo = {
    //    log info "making cluster compute info"
    val margin = getMargin
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
  }

  def findAndDispatchNewTasks(actionState: ActionStatus.ActionStatusType = DatabaseHelper.getActionStatus) = {
    handleTask(findNewTasks(actionState))
  }

  def handleTask(tasks: Seq[Task]) = {
    // dispatch or store in pending
    val workerPool = workers.filter(_._2.pendingTask < MaxTask)
    val pendingTasks = mutable.ListBuffer.empty[Task]
    tasks.foreach(task => {
      val (actorRef, record) = workerPool.minBy(_._2.pendingTask)
      if (record.pendingTask < MaxTask) {
        record.pendingTask += 1
        if (task.id == null) {
          DatabaseHelper.addNewTask(task, record.clusterSeedId, record.workerId)
        } else {
          DatabaseHelper.reassignTask(task.id, record.clusterSeedId, record.workerId)
        }
        actorRef ! task
      } else {
        pendingTasks += task
      }
    })
    addPendingTasks(pendingTasks)
  }


  def cleanTasks() = {
    val taskQuota: Long = workers.map(x => MaxTask - x._2.pendingTask).sum
    handleTask(getPendingTasks(Math.min(numberOfPendingTask, taskQuota)))
  }

  def findNewTasks(actionState: ActionStatus.ActionStatusType = DatabaseHelper.getActionStatus): Seq[Task] = {
    actionState match {
      case ActionStatus.preProcess =>
        //TODO resolve task from database
        DatabaseHelper.run(r => {
          //TODO
          r.table(Tables.RawData.name).without(Tables.RawData.Field)
        })
        Seq.empty
      case _ => log warning s"findTask on $actionState is not implemened"
        Seq.empty
    }
  }

  def getPendingTasks(limit: Long = -1): Seq[Task] = {
    val field = Tables.Task.Field
    val result: Cursor[ju.Map[String, AnyRef]] = DatabaseHelper.run(r => {
      val query = r.table(Tables.Task.name).filter(r.hashMap(field.pending.toString, true))
      if (limit > 0)
        query.limit(limit)
      query
    })
    result.iterator().asScala.map(record => {
      ActionStatus.withName(record.get(field.taskType.toString).toString) match {
        case ActionStatus.preProcess => PreProcessTask.fromMap(record)
      }
    }).toIndexedSeq
  }

  def numberOfPendingTask: Long =
    DatabaseHelper.run(r => r.table(Tables.Task.name).filter(r.hashMap(Tables.Task.Field.pending.toString, true)).count())


  def addPendingTasks(tasks: Seq[Task]) = {
    /*
    * 1. save new tasks
    * 2. update old tasks
    * */
    val table = Tables.Task.name
    val field = Tables.Task.Field
    val (newTasks, oldTasks) = tasks.partition(_.id == null)
    /* 1. save new tasks */
    DatabaseHelper.tableInsertRows(table, newTasks.asJava)
    /* 2. update old tasks */
    DatabaseHelper.run[ju.HashMap[String, AnyRef]](r => {
      val ids = r.args(oldTasks.map(_.id).asJava)
      val row = r.hashMap(field.pending.toString, true)
      r.table(table).getAll(ids).update(row)
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

  def getMargin: Long = System.currentTimeMillis - Main.config.getLong("clustering.report.timeout")
}
