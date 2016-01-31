package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import com.rethinkdb.RethinkDB.r
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.ImportFile
import hk.edu.polyu.datamining.pamap2.database.Tables
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.JavaConverters._


/**
  * Created by beenotung on 1/30/16.
  */
object UIActor {
  /* only reference to local instance */
  private var instance: UIActor = null

  def !(msg: Any) = instance.self ! msg
}

class UIActor extends Actor with ActorLogging {
  override def preStart = {
    UIActor.instance = this
    MonitorApplication.ready = true
    new Thread(() => {
      try {
        MonitorApplication.main(Array.empty)
      }
      catch {
        case e: IllegalStateException => log warning "restarting UIActor with existing JavaFX Application"
          MonitorController.restarted("UIActor is restarted")
      }
    }
    ).start()
  }

  override def receive: Receive = {
    case ImportFile(filename, lines) =>
      MonitorController.importingFile(filename)
      val records = lines
        .map(ImportActor.processLine)
        .map(_.`with`(Tables.RawData.Field.subject.toString, filename))
      r.table(Tables.RawData.name).insert(records.asJava)
      MonitorController.importedFile(filename)
      DispatchActor
    case msg =>
      log info s"received message : $msg"
      ???
  }
}
