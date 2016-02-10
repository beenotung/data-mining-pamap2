package hk.edu.polyu.datamining.pamap2.actor

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import hk.edu.polyu.datamining.pamap2.actor.ClusterInfo.{AskNodeInfo, ResponseNodeInfo}

object MonitorActor {
  val baseName = "Monitor-"
  private var subName: String = null

  def fullName: String =
    if (subName == null)
      throw new IllegalStateException("subName has not set")
    else
      baseName + subName

  def subName(value: String): Unit = subName = value
}

class MonitorActor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    log info "Starting ClusterMonitorActor"
    log info s"monitor path : ${self.path}"
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  // clean up on shutdown
  override def postStop(): Unit = cluster unsubscribe self


  // handle the member events
  def receive = {
    case MemberUp(member) => log info s"Member up ${member.address} with roles ${member.roles}"
    case UnreachableMember(member) => log warning s"Member unreachable ${member.address} with roles ${member.roles}"
    //case MemberRemoved(member, _) if member.address.equals(cluster.selfAddress) => log info "shutdown local JVM"
    //  System.exit(0)
    case MemberRemoved(member, previousStatus) => log info s"Member removed ${member.address} with roles ${member.roles}"
    case MemberExited(member) => log info s"Member exited ${member.address} with roles ${member.roles}"
    case AskNodeInfo =>
      val runtime: Runtime = Runtime.getRuntime
      sender ! ResponseNodeInfo(new Node(
        processor = runtime.availableProcessors(),
        freeMemory = runtime.freeMemory(),
        totalMemory = runtime.totalMemory(),
        maxMemory = runtime.maxMemory(),
        upTime = context.system.uptime,
        startTime = context.system.startTime
      ))
    case _: MemberEvent => // ignore
  }

}