package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.JavaConverters._

/**
  * Created by beenotung on 2/18/16.
  */
class WorkerActor extends Actor with ActorLogging {
  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterWorker(DatabaseHelper.clusterSeedId)
  }

  override def receive: Receive = {
    case task: Task => task match {
      case task: ExtractFromRaw => log info "extract from raw"
      case ProcessRawLines(filename, lines, fileType) =>
        lines.asScala.foreach(line => {
          val map = ImportActor.processLine(line)
          DatabaseHelper.run(r => r.table(Tables.RawData.name).insert(map))
        })
    }
      DatabaseHelper.finishTask(task.id)
      Dispatcher.proxy ! TaskCompleted(task.id)
    case ReBindDispatcher => preStart()
      log info "rebind dispatcher"
    case msg => log error s"Unsupported msg : $msg"
      ???
  }

  override def preStart = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId, workerId)
  }

  def workerId: String = DatabaseHelper.clusterSeedId + "@" + self.path.toStringWithAddress(self.path.address)
}
