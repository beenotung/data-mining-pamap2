package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.utils.Lang._

/**
  * Created by beenotung on 2/18/16.
  */
class WorkerActor extends Actor with ActorLogging {
  override def preStart = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId)
  }

  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterWorker(DatabaseHelper.clusterSeedId)
  }

  override def receive: Receive = {
    case msg => log error s"Unsupported msg : $msg"
      ???
  }
}
