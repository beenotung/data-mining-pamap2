package hk.edu.polyu.datamining.pamap2.actor

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, UnreachableMember}
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.{ComputeNodeHeartBeat, DispatcherHeartBeat, RequestNodeInfo, RestartCluster}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocolFactory.NodeInfo
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration

/**
  * Created by beenotung on 2/18/16.
  */
object ComputeActor {
  lazy val numberOfWorker = {
    val x = Main.config.getInt("compute.number_of_worker")
    if (x > 0) x else Runtime.getRuntime.availableProcessors()
  }
}

class ComputeActor extends CommonActor {
  val workers = mutable.Set.empty[ActorRef]
  var lastTimeDispatcherHeartBeat: Long = System.currentTimeMillis()
  var isDoingRestart = false

  override def preStart = {
    // init worker pool
    for (i <- 0 until ComputeActor.numberOfWorker) {
      log info s"start worker - $i"
      workers += context.actorOf(Props[WorkerActor])
    }
    // set repeat timer to report resources
    context.system.scheduler.schedule(Duration.Zero,
      Duration.create(ActorUtils.ReportInterval, TimeUnit.MILLISECONDS), self, ComputeNodeHeartBeat)

    //    cluster.subscribe(self,initialStateMode = InitialStateAsEvents,
    //      classOf[MemberEvent],classOf[UnreachableMember])
  }

  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterComputeNode(DatabaseHelper.clusterSeedId)
    workers.foreach(context.stop)
    workers.retain(_ => false)
    if (isDoingRestart)
      Main.mainRun()
  }

  override def receive: Receive = {
    case RestartCluster =>
      isDoingRestart = true
      system.terminate()
    case ComputeNodeHeartBeat => SingletonActor.Dispatcher.proxy ! NodeInfo.newInstance(system)
      if (lastTimeDispatcherHeartBeat < getMargin)
        workers.foreach(_ ! MessageProtocol.ReBindDispatcher)
    case DispatcherHeartBeat => lastTimeDispatcherHeartBeat = System.currentTimeMillis()
    case RequestNodeInfo => SingletonActor.Dispatcher.proxy ! NodeInfo.newInstance(context.system)

    case msg => showError(s"ComputeActor: Unsupported msg : $msg")
  }

}
