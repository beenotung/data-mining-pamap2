package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 1/21/16.
  */
object DispatchActor {
}

/**
  * this is design to be singleton
  */
class DispatchActor extends Actor with ActorLogging {
  override def preStart() = {
    log debug s"Starting ${getClass.getSimpleName}"
    self ! ActionStatus.checkStatus
  }

  override def receive: Receive = {
    case ActionStatus.checkStatus => self ! getCurrentActionStatus
    case ActionStatus.init => doInit()
    case ActionStatus.importing => doImport()
    case ActionStatus.preProcess => doPreProcess()
    case ActionStatus.learning => doLearning()
    case ActionStatus.testing => doTesting()
    case action: ActionStatus.Status => log debug s"received action request : $action"
    case msg => log debug s"received message : $msg"
  }

  def getCurrentActionStatus: ActionStatus.Status = {
    if (DatabaseHelper.hasInit)
      ActionStatus.importing
    else
      ActionStatus.init
  }

  def doInit() = {
    DatabaseHelper.init(ActionStatus.init.toString, ActionStatus.importing.toString)
    self ! ActionStatus.importing
  }

  def doImport() = {
  }

  def doPreProcess() = ???

  def doLearning() = ???

  def doTesting() = ???
}
