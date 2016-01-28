package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor._
import akka.routing.{ActorRefRoutee, Broadcast, RoundRobinRoutingLogic, Router}
import hk.edu.polyu.datamining.pamap2.actor.ComputeActor.Work

/**
  * Created by beenotung on 1/26/16.
  */
object ComputeActor {

  sealed trait Work {}

  case class Import(filename: String) extends Work

}

class ComputeActor extends Actor with ActorLogging {

  import akka.actor.SupervisorStrategy.{Restart, Resume}

  import scala.concurrent.duration._
  import scala.language.postfixOps

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
    case status: ActionStatus.Status => router.route(Broadcast(status), self)
    case w: Work => router.route(w, sender())
    case Terminated(actorRef) =>
      router = router removeRoutee actorRef
      val newActorRef = context actorOf Props[WorkerActor]
      context watch newActorRef
      router = router addRoutee actorRef
    case msg => log info s"Unsupported message : $msg"
  }
}

class WorkerActor extends Actor with ActorLogging {
  var concreteActor: ActorRef = null

  override def receive: Actor.Receive = {
    case status: ActionStatus.Status =>
      if (concreteActor != null)
        concreteActor ! PoisonPill
      status match {
        case ActionStatus.importing => concreteActor = context.actorOf(Props[ImportActor])
        case ActionStatus.preProcess => concreteActor = context.actorOf(Props[PreProcessDataActor])
        case ActionStatus.learning => ???
        case ActionStatus.testing => ???
        case _ => log info s"Unsupported ActionStatus : $status"
      }
    case w: Work =>
      if (concreteActor == null)
        throw new IllegalStateException("no concreteActor (has not received ActionStatus)")
      concreteActor forward w
    case msg => log info s"received message : $msg"
  }
}