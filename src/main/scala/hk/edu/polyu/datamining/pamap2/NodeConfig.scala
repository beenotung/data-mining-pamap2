package hk.edu.polyu.datamining.pamap2

import com.typesafe.config._
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.virtual.VirtualNetworkHelper

/**
  * This configuration is intended to run in a docker environment
  * It won't work
  */
case class NodeConfig(isSeed: Boolean = false, isPublic: Boolean = false, isCompute: Boolean = false, isUI: Boolean = false, ip: String = null, port: Int = 0) {

  import ConfigFactory._
  import NodeConfig._

  // Initialize the config once
  lazy val config = asConfig()

  // Name of the ActorSystem
  lazy val clusterName = config getString CLUSTER_NAME_PATH

  /**
    * @return config
    */
  private def asConfig(): Config = {
    val config = load(
      getClass.getClassLoader,
      ConfigResolveOptions.defaults.setAllowUnresolved(true)
    )
    val name = config getString CLUSTER_NAME_PATH

    // which config should be used
    val configPath =
      if (isSeed) SEED_NODE
      else if (isCompute) COMPUTE_NODE
      else if (isUI) UI_NODE
      else DEFAULT_NODE

    // use configured ip or get host ip if available
    //val ip = if (config hasPath "clustering.ip") config getString "clustering.ip" else HostIP.load getOrElse "127.0.0.1"
    val ipValue = ConfigValueFactory fromAnyRef {
      if (ip != null) ip
      else if (config hasPath "clustering.ip") config getString "clustering.ip"
      else if (isPublic) HostIP.PublicIP
      else HostIP.LocalIP
    }
    val portValue = ConfigValueFactory fromAnyRef {
      if (port != 0) port
      else if (config hasPath "clustering.port") config getString "clustering.port"
      else 0
    }


    // add seed nodes to config
    val seedNodesString = seedNodes.map { node =>
      s"""akka.cluster.seed-nodes += "akka.tcp://$name@$node""""
    }.mkString("\n")

    // build the final config and resolve it
    (ConfigFactory parseString seedNodesString)
      .withValue("clustering.ip", ipValue)
      .withValue("clustering.port", portValue)
      .withFallback(ConfigFactory parseResources configPath)
      .withFallback(config)
      .resolve
  }

}

object NodeConfig {
  /** static configuration for seed nodes */
  val SEED_NODE = "node.seed.conf"

  /** static configuration for compute nodes */
  val COMPUTE_NODE = "node.compute.conf"

  /** static configuration for ui nodes */
  val UI_NODE = "node.ui.conf"
  val DEFAULT_NODE = COMPUTE_NODE
  // IP:port of the seed nodes in the ActorSystem (from database)
  val seedNodes = {
    println("finding seed hosts...")
    val seedHosts = DatabaseHelper.findSeeds
    println(s"found seed hosts : $seedHosts")
    seedHosts.map(seed => s"${seed._1}:${seed._2}")
  }
  /** where to find the name of the ActorSystem */
  private val CLUSTER_NAME_PATH = "clustering.cluster.name"

  /**
    * @return NodeConfig
    * @throws IllegalStateException - if the cli parameters could not be parsed
    */
  def parse(args: Seq[String]): Option[NodeConfig] = {

    val parser = new scopt.OptionParser[NodeConfig]("akka-docker") {
      head("akka-docker", "2.3.4")

      opt[Unit]("seed") action { (_, c) =>
        c.copy(isSeed = true)
      } text "set this flag to start this system as a need node"

      opt[Unit]("public") action { (_, c) =>
        c.copy(isPublic = true)
      } text "set this flag to indicate this node on public network"

      opt[Unit]("local") action { (_, c) =>
        c.copy(isPublic = false)
      } text "set this flag to indicate this node on local network"

      opt[Unit]("compute") action { (_, c) =>
        c.copy(isCompute = true)
      } text "set this flag to start this system as a compute node"

      opt[Unit]("ui") action { (_, c) =>
        c.copy(isUI = true)
      } text "set this flag to start this system as a ui node"

      opt[Unit]("vm") action { (_, c) =>
        c.copy(ip = VirtualNetworkHelper.getHostIPFromVM)
      } text "set this flag to indicate this system is in virtual network"

      //arg[String]("<seed-node>...") unbounded() optional() action { (n, c) =>
      //  c.copy(seedNodes = c.seedNodes :+ n)
      //} text ("give a list of seed nodes like this: <ip>:<port> <ip>:<port>")

      checkConfig {
        case NodeConfig(false, _, _, _, _, _) if seedNodes.isEmpty => failure(s"this node require at least one seed node")
        case NodeConfig(true, false, _, _, _, _) if seedNodes.isEmpty =>
          reportWarning(s"this actor system is only accessible within local network")
          success
        case _ => success
      }
    }
    // parser.parse returns Option[C]
    parser.parse(args, NodeConfig())
  }
}