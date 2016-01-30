package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}

/**
  * Created by beenotung on 1/30/16.
  */
class UIActor extends Actor with ActorLogging {
  override def preStart = {
    hk.edu.polyu.datamining.pamap2.ui.MonitorApplication.main(Array.empty)
  }

  override def receive: Receive = {
    case msg => log info s"received message : $msg"
  }
}
