package hk.edu.polyu.datamining.pamap2.actor

/**
  * Created by beenotung on 2/18/16.
  */
object MessageProtocol {

  type ClusterComputeInfo = Seq[NodeInfo]

  sealed trait DispatchActorProtocol

  case class RegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  case class UnRegisterWorker(clusterSeedId: String) extends DispatchActorProtocol

  case class Report(nodeInfo: NodeInfo) extends DispatchActorProtocol

  case class Request[A]()

  case class Response[A](response: A)
}
