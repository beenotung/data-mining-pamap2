package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor._
import akka.routing.{ActorRefRoutee, Broadcast, RoundRobinRoutingLogic, Router}
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.Task.TaskType
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.{DispatchTask, FinishRangedTask, FinishTask, Task}
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.utils.Lang

import scala.collection.mutable

/**
  * Created by beenotung on 1/26/16.
  */
object DispatchActor {
  val PreferedTaskSize = DatabaseHelper.BestInsertCount

  sealed trait Task {
    val taskType: TaskType
  }

  case class RangedTask[TaskType >: Task](task: TaskType, range: Range)

  case class DispatchTask(task: TaskType)

  case class FinishTask(task: TaskType)

  case class DispatchRangedTask(task: TaskType, range: Range)

  case class FinishRangedTask(task: TaskType, range: Range)

  object Task extends Enumeration {
    type TaskType = Value
    val extractFromRawLine, itemCount = Value
  }

}

class GlobalDispatchActor extends Actor with ActorLogging {
  /* dispatched */
  val pendingTasks = mutable.HashMap.empty[TaskType, mutable.Set[Range]]
  /* not dispatched yet */
  val queuedTasks = mutable.HashMap.empty[TaskType, mutable.Set[Range]]

  val taskOwners = mutable.HashMap.empty[TaskType, mutable.Set[ActorRef]]

  override def preStart = {
    log info s"Starting ${getClass.getSimpleName}"
    log info s"The path of this ${getClass.getSimpleName} is ${self.path}"
  }

  override def receive: Actor.Receive = {
    case DispatchTask(task) =>
      if (!taskOwners.keySet.contains(task)) {
        /* new task */
        val start: Int = 0
        val end: Int = 1 //TODO get count from database
        queuedTasks.getOrElseUpdate(task, mutable.Set.empty) += Range(start, end)
        dispatch
      }
      taskOwners.getOrElseUpdate(task, mutable.Set.empty[ActorRef]) += sender
    case FinishTask(task) =>

    case FinishRangedTask(task, range) =>
      pendingTasks.get(task) match {
        case Some(ranges) =>
          if (ranges.contains(range))
            ranges -= range
          else
            Lang.remove(range)(ranges)
          if (ranges.isEmpty) {
            pendingTasks -= task
            taskOwners.remove(task) match {
              case Some(actors) =>
                val msg: FinishTask = FinishTask(task)
                actors.foreach(_ ! msg)
              case None => log error "finished task, but no task owners to report to"
            }
          }
          else
            dispatch
        case None =>
      }
    case msg => log error s"Unsupported message : $msg"
      ???
  }

  def dispatch = {}
}

import akka.actor.SupervisorStrategy.{Restart, Resume}

import scala.concurrent.duration._
import scala.language.postfixOps

class LocalDispatchActor extends Actor with ActorLogging {
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case e: ArithmeticException => Resume
      case e: NullPointerException => Restart
      case e: IllegalArgumentException => Stop
      case e: Exception => Escalate
    }
  var router = {
    val routees = Vector.fill(Runtime.getRuntime.availableProcessors()) {
      val actorRef = context actorOf Props[WorkerActor]
      context watch actorRef
      ActorRefRoutee(actorRef)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive = {
    case status: ActionState.ActionStatusType => router.route(Broadcast(status), self)
    case w: Task => router.route(w, sender())
    case Terminated(actorRef) =>
      router = router removeRoutee actorRef
      val newActorRef = context actorOf Props[WorkerActor]
      context watch newActorRef
      router = router addRoutee actorRef
    case msg => log error s"Unsupported message : $msg"
  }
}

class WorkerActor extends Actor with ActorLogging {
  var concreteActor: ActorRef = null

  override def receive: Actor.Receive = {
    case status: ActionState.ActionStatusType =>
      if (concreteActor != null)
        concreteActor ! PoisonPill
      status match {
        //        case ActionState.importing => concreteActor = context.actorOf(Props[ImportActor])
        case ActionState.preProcess => concreteActor = context.actorOf(Props[PreProcessDataActor])
        case ActionState.learning => ???
        case ActionState.testing => ???
        case _ => log error s"Unsupported ActionStatus : $status"
      }
    case w: Task =>
      if (concreteActor == null)
        throw new IllegalStateException("no concreteActor (has not received ActionStatus)")
      concreteActor forward w
    case msg => log error s"Unsupported message : $msg"
  }
}