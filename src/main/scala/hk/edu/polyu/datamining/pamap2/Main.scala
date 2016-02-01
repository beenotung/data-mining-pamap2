package hk.edu.polyu.datamining.pamap2

import akka.actor._
import akka.cluster.Cluster
import com.rethinkdb.RethinkDB
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.ui.MonitorController

object Main extends App {

  val nodeConfig = NodeConfig parse args

  // If a config could be parsed - start the system
  nodeConfig foreach { c =>
    val system = ActorSystem(c.clusterName, c.config)

    /* register self to database */
    val host = HostIP.all()
    val port = c.config.getInt("akka.remote.netty.tcp.port")
    val clusterSeedKey = DatabaseHelper.addSeed(host, port, RethinkDB.r.json({
      val s = c.config.toString
      s.substring(26, s.length - 2)
    })).get("generated_keys").asInstanceOf[java.util.List[String]].get(0)

    system.registerOnTermination({
      /* unregister self to database */
      DatabaseHelper.removeSeed(clusterSeedKey)
      /* notify UI */
      MonitorController.onActorSystemTerminated()
    })

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