package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.Main

/**
  * Created by beenotung on 2/18/16.
  */
object ActorUtils {
  lazy val ReportTimeout = Main.config.getLong("clustering.report.timeout")
  lazy val ReportInterval = Main.config.getLong("clustering.report.interval")
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

  def getMargin: Long = System.currentTimeMillis - ActorUtils.ReportTimeout
}