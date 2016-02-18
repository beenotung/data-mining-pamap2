package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, MemberStatus}
import hk.edu.polyu.datamining.pamap2.actor.DispatchActorProtocol.DispatchTask
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable


/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  /* only reference to local instance */
  private[actor] var instance: UIActor = null

  def !(msg: Any) = instance.self ! msg

  //def cluster: Cluster = instance.cluster
  def members = instance.cluster.state.members.filter(_.status == MemberStatus.Up)
}

class UIActor extends Actor with ActorLogging {
  import ActorUtils._
  val cluster = Cluster(context.system)
  var clusterInfoBuilder: ClusterInfoBuilder = null

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

  override def receive: Receive = {
    case command: DispatchTask => SingletonActor.GlobalDispatcher.proxy(context.system) ! command
    case StateActor.ResponseStatus(status) => MonitorController.receivedClusterStatus(status)
      log info "received status"
    case ClusterInfoProtocol.ResponseNodeInfo(node) => MonitorController.receivedNodeInfo(node)
      log info "received node info"
    case ClusterInfoProtocol.AskClusterInfo => log info "asking for status"
      /*
      * 1. ask cluster status
      * 2. ask nodes info (system resources)
      * */
      /* 1, ask cluster status */
      SingletonActor.StateHolder.proxy(context.system) ! StateActor.AskStatus
      /* 2. ask cluster members info */
      SingletonActor.GlobalDispatcher.proxy ! ClusterInfoProtocol.AskNodeInfo
    case msg =>
      log error s"unsupported message : $msg"
      ???
  }
}
