package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.{Cluster, MemberStatus}
import hk.edu.polyu.datamining.pamap2.actor.ActorProtocol.Ask
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable


/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  /* only reference to local instance */
  private[actor] var instance: UIActor = null

  private[actor]
  def !(msg: Any) = instance.self ! msg

  //def cluster: Cluster = instance.cluster
  def members = instance.cluster.state.members.filter(_.status == MemberStatus.Up)

  def requestUpdate = {
    UIActor ! ActorProtocol.ask[Clu]
  }
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
    case StateActor.AskStatus => SingletonActor.StateHolder.proxy ! StateActor.AskStatus
    case msg: Ask[ClusterInfoProtocol.ClusterInfo] => SingletonActor.GlobalDispatcher.proxy ! msg
    case msg =>
      log error s"unsupported message : $msg"
      ???
  }
}
