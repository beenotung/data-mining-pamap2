package hk.edu.polyu.datamining.pamap2.actor

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}

/**
  * Created by beenotung on 1/31/16.
  */
object SingletonActor {
  val SINGLETON = "singleton"

  sealed trait SingletonActorType {
    val name: String
    protected val actorProps: Props

    @deprecated
    def actorSelection(context: ActorContext): ActorSelection =
      context.actorSelection(context.self.path.root / "user" / name / SINGLETON)

    def init(system: ActorSystem) =
      system.actorOf(
        ClusterSingletonManager.props(
          actorProps,
          PoisonPill.getInstance,
          ClusterSingletonManagerSettings.create(system)
        ), name)

    def proxy(implicit system: ActorSystem) =
      system.actorOf(ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$name",
        settings = ClusterSingletonProxySettings.create(system)
      ))
  }

  case object Dispatcher extends SingletonActorType {
    override val name: String = "task-dispatcher"

    override val actorProps: Props = Props[DispatchActor]
  }

}
