package hk.edu.polyu.datamining.pamap2.actor

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorPath}

/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  var instance: UIActor = null
  case class ImportFile(file: File)
}

class UIActor extends Actor with ActorLogging {
  override def preStart = {
    UIActor.instance = this
    hk.edu.polyu.datamining.pamap2.ui.MonitorApplication.main(Array.empty)
  }

  override def receive: Receive = {
    case UIActor.ImportFile(file) =>
      val root = self.path.root
      log info s"The root is $root"
      context.system.actorSelection(ActorPath.fromString("akka://data-mining-pamap2/user/task-dispatcher/singleton"))
    case msg => log info s"received message : $msg"
      ???
  }
}
