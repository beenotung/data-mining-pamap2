package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import java.{util => ju}

import com.rethinkdb.model.MapObject

import scala.collection.JavaConverters._
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.som.{Som, Vector}

import scala.util.Random

/**
  * Created by beenotung on 2/18/16.
  */
object WorkerActor {
  lazy val IMUField = Tables.IMU.Field
  val IMUWeights: Vector = Main.config.getDoubleList("algorithm.som.weights").asScala.toArray.map(d => d.toDouble)
  val SomGridWidth = Main.config.getInt("algorithm.som.gridWidth")
  val SomGridHeight = Main.config.getInt("algorithm.som.gridHeight")

  /* TODO handle number format error */
  def toIMUVector(map: ju.Map[String, AnyRef]): Vector = {
    Array[Double](
      map.get(IMUField.temperature.toString).asInstanceOf[Double],
      map.get(IMUField.a16x.toString).asInstanceOf[Double],
      map.get(IMUField.a16y.toString).asInstanceOf[Double],
      map.get(IMUField.a16z.toString).asInstanceOf[Double],
      map.get(IMUField.a6x.toString).asInstanceOf[Double],
      map.get(IMUField.a6y.toString).asInstanceOf[Double],
      map.get(IMUField.a6z.toString).asInstanceOf[Double],
      map.get(IMUField.rx.toString).asInstanceOf[Double],
      map.get(IMUField.ry.toString).asInstanceOf[Double],
      map.get(IMUField.rz.toString).asInstanceOf[Double],
      map.get(IMUField.mx.toString).asInstanceOf[Double],
      map.get(IMUField.my.toString).asInstanceOf[Double],
      map.get(IMUField.mz.toString).asInstanceOf[Double])
  }
}

class WorkerActor extends CommonActor {
  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterWorker(DatabaseHelper.clusterSeedId)
  }

  override def receive: Receive = {
    case task: Task =>
      log.info(s"received task id: ${task.id}, $task")
      task match {
        case SOMProcessTask(bodyPart, count) =>
          try {
            log.info(s"start building som on $bodyPart")
            val som = new Som(
              weights = WorkerActor.IMUWeights,
              labelPrefix = bodyPart,
              initGrids = Som.randomGrids(WorkerActor.IMUWeights.length, WorkerActor.SomGridWidth, WorkerActor.SomGridHeight, -256d, 256d)
            )
            DatabaseHelper.getIMU(bodyPart, count).foreach(row => {
              som.addSample(WorkerActor.toIMUVector(row))
            })
            log.info(s"finish building som on $bodyPart, saving to database")
            DatabaseHelper.tableInsertRow(Tables.SomImage.name, som.toMap)
            log.info(s"som on $bodyPart saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case msg => showError(s"unsupported message: $msg")
      }
      log.info(s"finish task ${task.id}")
      DatabaseHelper.finishTask(task.id)
      Dispatcher.proxy ! TaskCompleted(task.id)
    case ReBindDispatcher => preStart()
      log info "rebind dispatcher"
    case msg => log warning s"Unsupported msg : $msg"
  }

  override def preStart = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId, workerId)
  }

  def workerId: String = DatabaseHelper.clusterSeedId + "@" + self.path.toStringWithAddress(self.path.address)
}
