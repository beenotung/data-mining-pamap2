package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.ImportFile
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.ui.{MonitorApplication, MonitorController}
import hk.edu.polyu.datamining.pamap2.utils.Lang.{fork, runnable}


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
    new Thread(runnable(() => {
      try {
        MonitorApplication.main(Array.empty)
        /* leave cluster when GUI window is closed by user */
        val cluster = Cluster(context.system)
        cluster.leave(cluster.selfAddress)
        System.exit(0)
      }
      catch {
        case e: IllegalStateException => log warning "restarting UIActor with existing JavaFX Application"
          MonitorController.restarted("UIActor is restarted")
      }
    })).start()
  }

  override def receive: Receive = {
    case ImportFile(filetype, filename, lines) => fork(() => importFile(filetype, filename, lines))
    case StateActor.ResponseStatus(status) => MonitorController.updateStatus(status)
      log info "received status"
    case StateActor.AskStatus => SingletonActor.StateHolder.proxy(context.system) ! StateActor.AskStatus
      log info "asking for status"
    case msg =>
      log error s"unsupported message : $msg"
      ???
  }

  private def importFile(fileType: FileType, filename: String, lines: Seq[String]): Unit = {
    MonitorController.importingFile(filename)
    //TODO send to workers ?
    val router = SingletonActor.GlobalDispatcher.actorSelection(context)
    lines.grouped(DatabaseHelper.BestInsertCount)
      .foreach(lines => router ! ImportFile(fileType, filename, lines))
    //val subjectField: String = Tables.RawData.Field.subject.toString
    //val tableName: String = Tables.RawData.name
    //lines.grouped(DatabaseHelper.BestInsertCount)
    //  .toParArray
    //  .foreach(lines => {
    //    val records = lines.map(ImportActor.processLine)
    //      .map(_.`with`(subjectField, filename))
    //    DatabaseHelper.insert(tableName, records.asJava)
    //  }
    //  )
    //log info "finished sending to database, result : $result"
    MonitorController.importedFile(filename)
  }
}
