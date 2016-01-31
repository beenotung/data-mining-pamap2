package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.actor.ActionState.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.StateActor.{GetStatus, NextStatus, SetStatus}
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 1/21/16.
  */
object ActionState extends Enumeration {
  type ActionStatusType = Value
  val checkStatus, init, importing, imported, preProcess, learning, testing, finished = Value

  def next(actionStatusType: ActionStatusType): ActionStatusType = actionStatusType match {
    case null => checkStatus
    //case `checkStatus` => init
    //case `init` => importing
    //case `importing` => imported
    //case `imported` => preProcess
    //case `preProcess` => learning
    //case `learning` => testing
    //case `testing` => finished
    case `finished` => finished
    case _ => Value(actionStatusType.id + 1)
  }
}

object StateActor {

  sealed trait Message

  case class GetStatus(actionState: ActionStatusType) extends Message

  case class SetStatus(actionState: ActionStatusType) extends Message

  case object GetStatus extends Message

  /** @deprecated */
  case object NextStatus extends Message

}

/**
  * this is design to be singleton
  */
class StateActor extends Actor with ActorLogging {
  var status: ActionStatusType = null

  override def preStart() = {
    log info s"Starting ${getClass.getSimpleName}"
    log info s"The path of this ${getClass.getSimpleName} is ${self.path}"
    self ! SetStatus(ActionState.checkStatus)
  }

  override def receive: Receive = {
    case GetStatus => sender ! status
    case NextStatus => self forward SetStatus(ActionState.next(status))
    case SetStatus(newStatus) => if (status == null || !status.equals(newStatus)) onStatusChanged(status, newStatus)
    case msg => log info s"received message : $msg"
      ???
  }

  def onStatusChanged(oldStatus: ActionStatusType, newStatus: ActionStatusType) = {
    status = newStatus
    val nextMessage: Option[StateActor.Message] = newStatus match {
      case ActionState.checkStatus => Some(SetStatus(findCurrentActionStatus))
      case ActionState.init => doInit()
        Some(NextStatus)
      case ActionState.importing => None
      case ActionState.imported => Some(NextStatus)
      case ActionState.preProcess => doPreProcess()
        ???
      case ActionState.learning => doLearning()
        ???
      case ActionState.testing => doTesting()
        ???
      case _ => None
    }
    nextMessage match {
      case Some(message) => self ! message
      case None =>
    }
  }

  def findCurrentActionStatus: ActionStatusType = {
    if (DatabaseHelper.hasInit)
      ActionState.importing
    else
      ActionState.init
  }

  def doInit() = {
    DatabaseHelper.init(ActionState.init.toString, ActionState.importing.toString)
  }

  def doPreProcess() = ???

  def doLearning() = ???

  def doTesting() = ???

  def doImport() = {
  }
}
