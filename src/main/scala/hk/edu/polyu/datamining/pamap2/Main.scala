package hk.edu.polyu.datamining.pamap2

import akka.actor._
import akka.cluster.Cluster
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor
import hk.edu.polyu.datamining.pamap2.ui.MonitorController

object Main extends App {

  val nodeConfig = NodeConfig parse args

  // If a config could be parsed - start the system
  nodeConfig foreach { c =>
    val system = ActorSystem(c.clusterName, c.config)

    system.registerOnTermination(MonitorController.onActorSystemTerminated)

    // Register a monitor actor for demo purposes
    system.actorOf(Props[actor.MonitorActor])

    system.log info s"ActorSystem ${system.name} started successfully"

    /* set singletons */
    SingletonActor.StateHolder.init(system)
    SingletonActor.GlobalDispatcher.init(system)

    if (Cluster(system).selfRoles.contains("seed")) {
      // TODO save IP to database?
    }
    else if (Cluster(system).selfRoles.contains("ui"))
    // register a UIActor
      system.actorOf(Props[actor.UIActor])
    else
    // register a ComputeActor
      system.actorOf(Props[actor.LocalDispatchActor])
  }

}