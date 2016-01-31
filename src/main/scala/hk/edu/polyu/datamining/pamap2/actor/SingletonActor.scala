package hk.edu.polyu.datamining.pamap2.actor

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}

/**
  * Created by beenotung on 1/31/16.
  */
object SingletonActor {
  val SINGLETON = "singleton"

  sealed trait SingletonActorType[A >: Actor] {
    val name: String

    def actorSelection(context: ActorContext): ActorSelection =
      context.actorSelection(context.self.path.root / "user" / name / SINGLETON)

    def init(system: ActorSystem) =
      system.actorOf(
        ClusterSingletonManager.props(
          Props[A],
          PoisonPill.getInstance,
          ClusterSingletonManagerSettings.create(system)
        ), name)
  }

  case object StateHolder extends SingletonActorType {
    override val name: String = "state-holder"
  }

  case object GlobalDispatcher extends SingletonActorType {
    override val name: String = "global-dispatcher"
  }

}
