package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.ActorSystem
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 2/8/16.
  */
class Usage[A <: AnyVal](val used: A, val total: A)

class IntUsage(override val used: Int, override val total: Int) extends Usage[Int](used, total)

class LongUsage(override val used: Long, override val total: Long) extends Usage[Long](used, total)

case class NodeInfo(val processor: Int, val freeMemory: Long, val totalMemory: Long, val maxMemory: Long, val upTime: Long, val startTime: Long, val clusterSeedId: String) extends Comparable[NodeInfo] {
  override def compareTo(o: NodeInfo): Int = clusterSeedId.compareTo(o.clusterSeedId)
}

object NodeInfo {
  def newInstance(implicit system: ActorSystem): NodeInfo = {
    val runtime = Runtime.getRuntime
    new NodeInfo(
      processor = runtime.availableProcessors(),
      freeMemory = runtime.freeMemory(),
      totalMemory = runtime.totalMemory(),
      maxMemory = runtime.maxMemory(),
      upTime = system.uptime,
      startTime = system.startTime,
      clusterSeedId = DatabaseHelper.clusterSeedId
    )
  }
}
