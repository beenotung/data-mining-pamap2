package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import hk.edu.polyu.datamining.pamap2.actor.ClusterInfo.AskNodeInfo
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.DispatchTask
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable


/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  /* only reference to local instance */
  private[actor] var instance: UIActor = null

  def !(msg: Any) = instance.self ! msg
}

class UIActor extends Actor with ActorLogging {
  override def preStart = {
    UIActor.instance = this
    MonitorApplication.ready = true
    new Thread(runnable(() => {
      try {
        MonitorApplication.main(Array.empty)
        /* leave cluster when GUI window is closed by user */
        val cluster = Cluster(context.system)
        cluster.leave(cluster.selfAddress)
        System.exit(0)
      }
      catch {
        case e: IllegalStateException => log warning "restarting UIActor with existing JavaFX Application"
          MonitorController.restarted("UIActor is restarted")
      }
    })).start()
  }

  var clusterInfoBuilder: ClusterInfoBuilder = null

  override def receive: Receive = {
    case command: DispatchTask => SingletonActor.GlobalDispatcher.proxy(context.system) ! command
    case StateActor.ResponseStatus(status) => MonitorController.receivedClusterStatus(status)
      log info "received status"
    case ClusterInfo.ResponseNodeInfo(node) => MonitorController.receivedNodeInfo(sender().path.address.toString, node)
      log info "received node info"
    case ClusterInfo.AskClusterInfo => log info "asking for status"
      /*
      * 1. ask cluster status
      * 2. ask cluster members info
      *    1. number of nodes
      *    2. processor usage
      *    3. memory usage
      *    4. task (pending and completed)
      * */
      clusterInfoBuilder = new ClusterInfoBuilder
      /* 1, ask cluster status */
      SingletonActor.StateHolder.proxy(context.system) ! StateActor.AskStatus
      /* 2. ask cluster members info */
      //TODO update nodes count
      context.actorSelection(s"/user/${MonitorActor.baseName}*").tell(AskNodeInfo, self)
    case msg =>
      log error s"unsupported message : $msg"
      ???
  }

  val cluster = Cluster(context.system)
}
