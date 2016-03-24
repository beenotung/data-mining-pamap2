package hk.edu.polyu.datamining.pamap2.actor

import akka.cluster.{Cluster, MemberStatus}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
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
    SingletonActor.Dispatcher.proxy(instance.context.system) ! msg
  }

  private[actor]
  def !(msg: Any) = instance.self ! msg
}

class UIActor extends CommonActor {

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
    case RequestClusterComputeInfo => SingletonActor.Dispatcher.proxy ! RequestClusterComputeInfo
    //      log info "asking cluster info"
    case ClusterComputeInfo(nodeInfos) => MonitorController.receivedNodeInfos(nodeInfos)
    //      log info "received cluster info"
    case msg =>
      showError(s"unsupported message : $msg")
  }
}
