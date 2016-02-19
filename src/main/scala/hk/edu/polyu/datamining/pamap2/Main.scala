package hk.edu.polyu.datamining.pamap2

import akka.actor._
import akka.cluster.Cluster
import com.rethinkdb.RethinkDB
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.ui.MonitorController
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable

import scala.collection.JavaConverters._

object Main extends App {

  val nodeConfig = NodeConfig parse args

  // If a config could be parsed - start the system
  nodeConfig foreach { c =>
    val system = ActorSystem(c.clusterName, c.config)
    val cluster: Cluster = Cluster(system)

    /* register self to database */
    val host = cluster.selfAddress.host.get
    val port = cluster.selfAddress.port.getOrElse(c.config.getInt("akka.remote.netty.tcp.port"))
    val roles = cluster.selfRoles.toIndexedSeq.asJava
    val clusterSeedId = DatabaseHelper.addSeed(host, port, roles, RethinkDB.r.json({
      val s = c.config.toString
      s.substring(26, s.length - 2)
    }))

    /* set shutdown hook */
    Runtime.getRuntime.addShutdownHook(new Thread(runnable(() => {
      /* unregister self to database */
      DatabaseHelper.removeSeed(clusterSeedId)
    })))
    system.registerOnTermination({
      /* notify UI, will shutdown JVM */
      MonitorController.onActorSystemTerminated()
      //shutdown jvm now?
    })

    // Register a monitor actor for demo purposes
    //MonitorActor.subName(clusterSeedId)
    //system.actorOf(Props[actor.MonitorActor], MonitorActor.fullName)

    system.log info s"ActorSystem ${system.name} started successfully"

    /* set singletons */
    SingletonActor.StateHolder.init(system)
    SingletonActor.Dispatcher.init(system)

    if (cluster.selfRoles.contains("seed")) {
      // register self to seed node list (akka)
      cluster.join(cluster.selfAddress)
    }
    else if (cluster.selfRoles.contains("ui"))
    // register a UIActor
      system.actorOf(Props[actor.UIActor])
    else
    // register a ComputeActor
      system.actorOf(Props[actor.ComputeActor])
  }
  def config=nodeConfig.get.config
}