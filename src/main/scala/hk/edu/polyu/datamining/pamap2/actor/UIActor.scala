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
