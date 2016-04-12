package hk.edu.polyu.datamining.pamap2.actor

import javafx.application.Platform

import akka.cluster.{Cluster, MemberStatus}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.{ResponseClusterComputeInfo, RestartCluster}
import hk.edu.polyu.datamining.pamap2.actor.UIActor.DispatchMessage
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang
import hk.edu.polyu.datamining.pamap2.utils.Lang._


/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  /* only reference to local instance */
  private[actor] var instance: UIActor = null

  //def cluster: Cluster = instance.cluster
  def members = instance.cluster.state.members.filter(_.status == MemberStatus.Up)

  def dispatch(msg: Any): Unit = {
    instance.self ! DispatchMessage(msg)
  }

  private[actor]
  def !(msg: Any) = instance.self ! msg

  private case class DispatchMessage(msg: Any)

}

class UIActor extends CommonActor {
  var isDoingRestart = false

  override def postStop = {
    if (isDoingRestart)
      Platform.runLater(() => MonitorApplication.getStage.close())
  }

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
          //          if (!isDoingRestart) {
          cluster.leave(cluster.selfAddress)
          System.exit(0)
          //          }
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
    case RestartCluster => SingletonActor.Dispatcher.proxy ! RestartCluster
      isDoingRestart = true
      system.terminate()
    case DispatchMessage(msg) => SingletonActor.Dispatcher.proxy ! msg
    //    case RequestClusterComputeInfo => SingletonActor.Dispatcher.proxy ! RequestClusterComputeInfo
    //      log info "asking cluster info"
    case ResponseClusterComputeInfo(clusterComputeInfo) => MonitorController.receivedNodeInfos(clusterComputeInfo)
    //      log info "received cluster compute info"
    case msg =>
      showError(s"unsupported message : $msg")
  }
}
