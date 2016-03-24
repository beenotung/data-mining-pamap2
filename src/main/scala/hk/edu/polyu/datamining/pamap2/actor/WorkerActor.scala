package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.utils.Lang._

/**
  * Created by beenotung on 2/18/16.
  */
class WorkerActor extends CommonActor {
  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterWorker(DatabaseHelper.clusterSeedId)
  }

  override def receive: Receive = {
    case task: Task => task match {
      case PreProcessTask(skip, limit) =>
      case msg => showError(s"unsupported message: $msg")
    }
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
