package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.ActorSystem

import scala.collection.mutable

/**
  * Created by beenotung on 2/8/16.
  */
object ClusterInfoProtocol {

  case class ResponseClusterInfo(val clusterInfo: ClusterInfo)

  case class ResponseNodeInfo(val node: Node)

  object AskClusterInfo

  object AskNodeInfo

}

class ClusterInfo(val nodes: Seq[Node], val clusterUptime: Long)

class ClusterInfoBuilder {
  val nodes = mutable.Buffer.empty[Node]

  def +(node: Node) = nodes.+=(node)

  def build(system: ActorSystem): ClusterInfo = new ClusterInfo(nodes.toIndexedSeq, system.uptime)
}

class Usage[A <: AnyVal](val used: A, val total: A)

class IntUsage(override val used: Int, override val total: Int) extends Usage[Int](used, total)

class LongUsage(override val used: Long, override val total: Long) extends Usage[Long](used, total)

case class Node(val processor: Int, val freeMemory: Long, val totalMemory: Long, val maxMemory: Long, val upTime: Long, val startTime: Long, val clusterSeedId: String) extends Comparable[Node] {
  override def compareTo(o: Node): Int = clusterSeedId.compareTo(o.clusterSeedId)
}
