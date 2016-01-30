package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor._
import akka.routing.{RoundRobinRoutingLogic, Routee, Router}
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.Register
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 1/21/16.
  */
object DispatchActor {
  val Name = "task-dispatcher"

  def getActorSelection(context: ActorContext): ActorSelection =
    context.actorSelection(context.self.path.root / "user" / Name / "singleton")

  sealed trait Command

  object Register extends Command

}

/**
  * this is design to be singleton
  */
class DispatchActor extends Actor with ActorLogging {

  import scala.concurrent.duration._
  import scala.language.postfixOps

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: ArithmeticException => Resume
      case e: NullPointerException => Restart
      case e: IllegalArgumentException => Stop
      case e: Exception => Escalate
    }
  var routeeActorRefs = Set.empty[ActorRef]
  var router = {
    Router(RoundRobinRoutingLogic(), Vector.empty[Routee])
  }

  override def preStart() = {
    log info s"Starting ${getClass.getSimpleName}"
    log info s"The path of this ${getClass.getSimpleName} is ${self.path}"
    self ! ActionStatus.checkStatus
  }

  var idealRoutees=Set.empty[ActorRef]

  def dispatch(msg: Any) = {
    router route(msg, sender)
  }

  override def receive: Receive = {
    case ActionStatus.checkStatus => self ! getCurrentActionStatus
    case ActionStatus.init => doInit()
    case ActionStatus.importing => doImport()
    case ActionStatus.preProcess => doPreProcess()
    case ActionStatus.learning => doLearning()
    case ActionStatus.testing => doTesting()
    case Register =>
      context watch sender
      router addRoutee sender
      routeeActorRefs += sender
    case Terminated(actorRef) =>
      context unwatch actorRef
      router removeRoutee actorRef
      routeeActorRefs -= actorRef
    case work: ComputeActor.Work => dispatch(work)
    case action: ActionStatus.Status => log debug s"received action request : $action"
    case msg =>
      log info s"received unsuported message : $msg"
      ???
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
