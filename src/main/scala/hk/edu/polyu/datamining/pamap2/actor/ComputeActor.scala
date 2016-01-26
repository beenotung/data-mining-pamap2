package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor._
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
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
      case ArithmeticException => Resume
      case NullPointerException => Restart
      case IllegalArgumentException => Stop
      case Exception => Escalate
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
    case w: Work => router.route(w, sender())
    case Terminated(actorRef) =>
      router = router removeRoutee actorRef
      val newActorRef = context actorOf Props[WorkerActor]
      context watch newActorRef
      router = router addRoutee actorRef
    case msg => log info s"received message : $msg"
  }
}

class WorkerActor extends Actor with ActorLogging {
  override def receive: Actor.Receive = {
    case w: Work =>
    case msg => log info s"received message : $msg"
  }
}