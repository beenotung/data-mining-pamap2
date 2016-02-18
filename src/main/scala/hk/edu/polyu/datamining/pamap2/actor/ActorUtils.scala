package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{ActorContext, ActorSystem}
import akka.cluster.Cluster

/**
  * Created by beenotung on 2/18/16.
  */
object ActorUtils {
  implicit def cluster(implicit system: ActorSystem) = Cluster(system)

  implicit def system(implicit context: ActorContext) = context.system
}

object ActorProtocol {

  case class Ask[A]() {
  }

  def ask[A] = new Ask[A]

  case class Response[A](val response: A)

}
