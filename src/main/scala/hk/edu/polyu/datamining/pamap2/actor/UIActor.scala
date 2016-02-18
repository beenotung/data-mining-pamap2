package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, MemberStatus}
import hk.edu.polyu.datamining.pamap2.actor.StateActor.ResponseStatus
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang
import hk.edu.polyu.datamining.pamap2.utils.Lang.{runnable, _}

/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  /* only reference to local instance */
  private[actor] var instance: UIActor = null

  //def cluster: Cluster = instance.cluster
  def members = instance.cluster.state.members.filter(_.status == MemberStatus.Up)

  def requestUpdate(): Unit = {
    UIActor ! StateActor.AskStatus
    UIActor ! MessageProtocol.Request[MessageProtocol.ClusterComputeInfo]()
  }

  private[actor]
  def !(msg: Any) = instance.self ! msg
}

class UIActor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart = {
    UIActor.instance = this
    MonitorApplication.ready = true
    Lang.fork(() => {
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
    })
  }

  override def receive: Receive = {
    case StateActor.AskStatus => SingletonActor.StateHolder.proxy ! StateActor.AskStatus
    case ResponseStatus(status) => MonitorController.receivedClusterStatus(status)
    case msg: MessageProtocol.Request[MessageProtocol.ClusterComputeInfo] => SingletonActor.Dispatcher.proxy ! msg
    case msg: MessageProtocol.ClusterComputeInfo => MonitorController.receivedNodeInfos(msg)
    case msg =>
      log error s"unsupported message : $msg"
      ???
  }
}
