package hk.edu.polyu.datamining.pamap2.actor

/**
  * Created by beenotung on 2/18/16.
  */
object MessageProtocol {

  case class ClusterComputeInfo(nodeInfo: Seq[NodeInfo])

  sealed trait Request

  case object RequestClusterComputeInfo extends Request

  case object RequestNodeInfo extends Request

  sealed trait DispatchActorProtocol

  case class RegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  case class UnRegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  sealed trait Task

  case class ExtractFromRaw(ids: Seq[String]) extends Task

}
