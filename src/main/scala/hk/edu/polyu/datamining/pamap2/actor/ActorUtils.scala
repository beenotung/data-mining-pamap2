package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}

/**
  * Created by beenotung on 2/18/16.
  */
object ActorUtils {
}

trait CommonActor extends Actor with ActorLogging {
  def showError(x: Any*): Unit = {
    if (x.length == 1) {
      log error x.head.toString
      System.err.println(x.head)
    } else {
      log error x.toString
      System.err.println(x)
    }
  }
}