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

    @deprecated
    def actorSelection(context: ActorContext): ActorSelection =
      context.actorSelection(context.self.path.root / "user" / name / SINGLETON)

    protected def actorProps: Props

    def init(system: ActorSystem) =
      system.actorOf(
        ClusterSingletonManager.props(
          actorProps,
          PoisonPill.getInstance,
          ClusterSingletonManagerSettings.create(system)
        ), name)

    def proxy(system: ActorSystem) =
      system.actorOf(ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$name",
        settings = ClusterSingletonProxySettings.create(system)
      ))
  }

  case object StateHolder extends SingletonActorType {
    override val name: String = "state-holder"

    override def actorProps: Props = Props[StateActor]
  }

  case object GlobalDispatcher extends SingletonActorType {
    override val name: String = "global-dispatcher"

    override def actorProps: Props = Props[GlobalDispatchActor]
  }

}
