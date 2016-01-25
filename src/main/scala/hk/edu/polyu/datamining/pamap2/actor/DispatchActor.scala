package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.CheckStatus
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 1/21/16.
  */
object DispatchActor {

  sealed trait ActionStatus {
    def name: String = getClass.getSimpleName
  }

  case object CheckStatus extends ActionStatus

  case object Init extends ActionStatus

  case object Import extends ActionStatus

  case object PreProcess extends ActionStatus

  case object Learning extends ActionStatus

  case object Testing extends ActionStatus

}

/**
  * this is design to be singleton
  */
class DispatchActor extends Actor with ActorLogging {
  override def preStart() = {
    log debug s"Starting ${getClass.getSimpleName}"
    self ! CheckStatus
    //    new Thread(() => {
    //      //TODO
    //      log debug "***************************************************"
    //      log debug s"current Action Status = ${getCurrentActionStatus}"
    //      log debug "***************************************************"
    //    }).start()
  }

  def getCurrentActionStatus: DispatchActor.ActionStatus = {
    if (DatabaseHelper.hasInit)
      DispatchActor.Import
    else
      DispatchActor.Init
  }

  def doInit = ???

  def doImport = ???

  def doPreProcess = ???

  def doLearning = ???

  def doTesting = ???

  override def receive: Receive = {
    case CheckStatus => log debug s"current Action Status = $getCurrentActionStatus"
    case msg => log debug s"received message : $msg"
  }
}
