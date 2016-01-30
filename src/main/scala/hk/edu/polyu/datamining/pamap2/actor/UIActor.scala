package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging, Props}
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.ImportFile
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable

/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  var instance: UIActor = null


}

class UIActor extends Actor with ActorLogging {
  override def preStart = {
    UIActor.instance = this
    MonitorApplication.ready = true
    new Thread(() => {
      MonitorApplication.main(Array.empty)
    }
    ).start()
  }

  override def receive: Receive = {
    case msg: ImportFile => context.actorOf(Props[ImportActor]) ! msg
    case ProcessingImport(filename)=>MonitorController.instance.importingFile(filename)
    case FinishedImport(filename) => MonitorController.instance.importedFile(filename)
    case msg =>
      log info s"received message : $msg"
      ???
  }
}
