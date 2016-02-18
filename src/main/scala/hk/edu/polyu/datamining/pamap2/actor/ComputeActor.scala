package hk.edu.polyu.datamining.pamap2.actor

import java.util.concurrent.TimeUnit

import akka.actor._
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.Request
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration

/**
  * Created by beenotung on 2/18/16.
  */
class ComputeActor extends Actor with ActorLogging {
  val workers = mutable.Set.empty[ActorRef]

  override def preStart = {
    // init worker pool
    for (i <- 0 until Runtime.getRuntime.availableProcessors()) {
      log info s"start worker - $i"
      workers += context.actorOf(Props[WorkerActor])
    }
    // set repeat timer to report resources
    val timer: Cancellable = context.system.scheduler.schedule(Duration.Zero,
      Duration.create(50, TimeUnit.MILLISECONDS), self, MessageProtocol.Request[NodeInfo]())
  }

  override def postStop = {

  }

  override def receive: Receive = {
    case msg: Request[NodeInfo] => SingletonActor.Dispatcher.proxy ! NodeInfo.newInstance(context.system)
    case msg => log error s"Unsupported msg : $msg"
      ???
  }
}
