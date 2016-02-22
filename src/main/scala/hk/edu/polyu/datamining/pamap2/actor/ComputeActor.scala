package hk.edu.polyu.datamining.pamap2.actor

import java.util.concurrent.TimeUnit

import akka.actor._
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.RequestNodeInfo
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocolFactory.NodeInfo
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
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
      Duration.create(Main.config.getLong("clustering.report.interval"), TimeUnit.MILLISECONDS), self, RequestNodeInfo)
  }

  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterComputeNode(DatabaseHelper.clusterSeedId)
    workers.foreach(context.stop)
    workers.retain(_ => false)
  }

  override def receive: Receive = {
    case RequestNodeInfo => SingletonActor.Dispatcher.proxy ! NodeInfo.newInstance(context.system)

    case msg => log error s"Unsupported msg : $msg"
      ???
  }
}
