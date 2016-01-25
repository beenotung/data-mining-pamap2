package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.ActionStatus
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper._

/**
  * Created by beenotung on 1/21/16.
  */
object DispatchActor {

  object ActionStatus extends Enumeration {
    type Status = Value
    val checkStatus, init, importing, preProcess, learning, testing = Value
  }

}

/**
  * this is design to be singleton
  */
class DispatchActor extends Actor with ActorLogging {
  override def preStart() = {
    log debug s"Starting ${getClass.getSimpleName}"
    self ! ActionStatus.checkStatus
  }

  def getCurrentActionStatus: DispatchActor.ActionStatus.Status = {
    if (DatabaseHelper.hasInit)
      ActionStatus.importing
    else
      ActionStatus.init
  }

  def doInit ={
    DatabaseHelper.init()
    r.dbList().conta
  }

  def doImport = ???

  def doPreProcess = ???

  def doLearning = ???

  def doTesting = ???

  override def receive: Receive = {
    case ActionStatus.checkStatus => self ! getCurrentActionStatus
    case ActionStatus.init => doInit
    case ActionStatus.importing => doImport
    case ActionStatus.preProcess => doPreProcess
    case ActionStatus.learning => doLearning
    case ActionStatus.testing => doTesting
    case action: ActionStatus.Status => log debug s"received action request : $action"
    case msg => log debug s"received message : $msg"
  }
}
