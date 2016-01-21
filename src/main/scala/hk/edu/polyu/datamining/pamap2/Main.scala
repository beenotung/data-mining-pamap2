package hk.edu.polyu.datamining.pamap2

import akka.actor._
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}

object Main extends App {

  val nodeConfig = NodeConfig parse args

  // If a config could be parsed - start the system
  nodeConfig map { c =>
    val system = ActorSystem(c.clusterName, c.config)

    // Register a monitor actor for demo purposes
    system.actorOf(Props[actor.MonitorActor], "cluster-monitor")

    system.log info s"ActorSystem ${system.name} started successfully"

    // set DispatchActor singleton
    system.actorOf(
      ClusterSingletonManager.props(
        Props[actor.DispatchActor],
        PoisonPill.getInstance,
        ClusterSingletonManagerSettings.create(system)
      ), "task-dispatcher")
  }

}