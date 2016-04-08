package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import hk.edu.polyu.datamining.pamap2.utils.Log
import java.{util => ju}

import com.rethinkdb.net.Cursor

/**
  * Created by beenotung on 2/18/16.
  */
class WorkerActor extends CommonActor {
  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterWorker(DatabaseHelper.clusterSeedId)
  }

  override def receive: Receive = {
    case task: Task =>
      log.info(s"received task id: ${task.id}, $task")
      task match {
        case PreProcessTask(skip, limit) =>
          val f = Tables.RawData.Field.isTrain.toString
          val fs = Tables.RawData.Field
          val cursor: Cursor[ju.Map[String, AnyRef]] = DatabaseHelper.run(r => r.table(Tables.RawData.name).filter(r.hashMap(f, true)).skip(skip).limit(limit))
          cursor.forEach(consumer(row => {
            val activityId = row.get(fs.activityId.toString).asInstanceOf[String]
            val timeSequences = row.get(fs.timeSequence.toString).asInstanceOf[ju.List[ju.Map[String, AnyRef]]]
            val subject = row.get(fs.subject.toString).asInstanceOf[String]
          }))
        case msg => showError(s"unsupported message: $msg")
      }
      log.info(s"finish task ${task.id}")
      DatabaseHelper.finishTask(task.id)
      Dispatcher.proxy ! TaskCompleted(task.id)
    case ReBindDispatcher => preStart()
      log info "rebind dispatcher"
    case msg => log warning s"Unsupported msg : $msg"
  }

  override def preStart = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId, workerId)
  }

  def workerId: String = DatabaseHelper.clusterSeedId + "@" + self.path.toStringWithAddress(self.path.address)
}
