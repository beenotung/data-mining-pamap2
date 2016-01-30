package hk.edu.polyu.datamining.pamap2

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import hk.edu.polyu.datamining.pamap2.actor.{StateActor, StateActor$}

object Main extends App {

  val nodeConfig = NodeConfig parse args

  // If a config could be parsed - start the system
  nodeConfig map { c =>
    val system = ActorSystem(c.clusterName, c.config)

    // Register a monitor actor for demo purposes
    system.actorOf(Props[actor.MonitorActor])

    system.log info s"ActorSystem ${system.name} started successfully"

    if (Cluster(system).selfRoles.contains("seed"))
    // set StateActor singleton
      system.actorOf(
        ClusterSingletonManager.props(
          Props[actor.StateActor],
          PoisonPill.getInstance,
          ClusterSingletonManagerSettings.create(system)
        ), StateActor.Name)
    else if (Cluster(system).selfRoles.contains("ui"))
    // register a UIActor
      system.actorOf(Props[actor.UIActor])
    else
    // register a ComputeActor
      system.actorOf(Props[actor.LocalDispatchActor])
  }

}