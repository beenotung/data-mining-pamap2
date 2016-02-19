package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, MemberStatus}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
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
    UIActor ! RequestClusterComputeInfo
  }

  private[actor]
  def !(msg: Any) = instance.self ! msg

  def onImportedRawFile(): Unit = {
    UIActor ! ExtractFromRaw
  }
}

class UIActor extends Actor with ActorLogging {

  override def preStart = {
    UIActor.instance = this
    if (!cluster.selfRoles.contains("ui")) {
      context.stop(self)
    } else {
      MonitorApplication.ready = true
      Lang.fork(() => {
        try {
          MonitorApplication.main(Array.empty)
          /* leave cluster when GUI window is closed by user */
          cluster.leave(cluster.selfAddress)
          System.exit(0)
        }
        catch {
          case e: IllegalStateException => log warning "restarting UIActor with existing JavaFX Application"
            MonitorController.restarted("UIActor is restarted")
        }
      })
    }
  }

  def cluster = Cluster(context.system)

  override def receive: Receive = {
    case StateActor.AskStatus => SingletonActor.StateHolder.proxy ! StateActor.AskStatus
      log info "asking status"
    case ResponseStatus(status) => MonitorController.receivedClusterStatus(status)
      log info "received status"
    case RequestClusterComputeInfo => SingletonActor.Dispatcher.proxy ! RequestClusterComputeInfo
      log info "asking cluster info"
    case ClusterComputeInfo(nodeInfos) => MonitorController.receivedNodeInfos(nodeInfos)
      log info "received cluster info"
    case ExtractFromRaw => SingletonActor.Dispatcher.proxy ! ExtractFromRaw
      log info "sent extract-from-raw to dispatcher"
    case msg =>
      log error s"unsupported message : $msg"
      ???
  }
}
