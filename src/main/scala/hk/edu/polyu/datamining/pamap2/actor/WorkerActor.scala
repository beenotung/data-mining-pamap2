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
import hk.edu.polyu.datamining.pamap2.utils.Log

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
      map.get(IMUField.temperature.toString).toString.toDouble,
      map.get(IMUField.a16x.toString).toString.toDouble,
      map.get(IMUField.a16y.toString).toString.toDouble,
      map.get(IMUField.a16z.toString).toString.toDouble,
      map.get(IMUField.a6x.toString).toString.toDouble,
      map.get(IMUField.a6y.toString).toString.toDouble,
      map.get(IMUField.a6z.toString).toString.toDouble,
      map.get(IMUField.rx.toString).toString.toDouble,
      map.get(IMUField.ry.toString).toString.toDouble,
      map.get(IMUField.rz.toString).toString.toDouble,
      map.get(IMUField.mx.toString).toString.toDouble,
      map.get(IMUField.my.toString).toString.toDouble,
      map.get(IMUField.mz.toString).toString.toDouble)
  }
}

class WorkerActor extends CommonActor {
  override def postStop = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.UnRegisterWorker(DatabaseHelper.clusterSeedId)
  }

  var lastIMUIds: String = null
  var handSom, ankleSom, chestSom: Som = null

  override def receive: Receive = {
    case task: Task =>
      log.info(s"received task id: ${task.id}, $task")
      task match {
        case SOMProcessTask(bodyPart, count) =>
          try {
            Log.info(s"start building som on $bodyPart")
            val som = new Som(
              weights = WorkerActor.IMUWeights,
              labelPrefix = bodyPart,
              initGrids = Som.randomGrids(WorkerActor.IMUWeights.length, WorkerActor.SomGridWidth, WorkerActor.SomGridHeight, -256d, 256d)
            )
            DatabaseHelper.getIMU(bodyPart, count, Tables.RawData.Field.isTrain.toString).foreach(row => {
              som.addSample(WorkerActor.toIMUVector(row))
            })
            Log.info(s"finish building som on $bodyPart, saving to database")
            DatabaseHelper.tableInsertRow(Tables.SomImage.name, som.toMap)
            Log.info(s"som on $bodyPart saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case ItemCountTask(imuIds, offset) =>
          val fs = Tables.RawData.Field
          try {
            DatabaseHelper.run(r => r.table(Tables.RawData.name)
              .filter(fs.isTrain.toString, true)
              .skip(offset)
              .limit(1)
            ).asInstanceOf[ju.List[MapObject[String, AnyRef]]]
              .forEach(consumer(row => {
                if (lastIMUIds != imuIds) {
                  DatabaseHelper.run(r => r.table(Tables.SomImage.name)).asInstanceOf[ju.List[ju.Map[String, AnyRef]]]
                    .forEach(consumer(row => row.get(Tables.SomImage.LabelPrefix) match {
                      case fs.ankle.toString => ankleSom = Som.fromMap(row).get
                      case fs.hand.toString => handSom = Som.fromMap(row).get
                      case fs.chest.toString => chestSom = Som.fromMap(row).get
                    }))
                }
                row.`with`(a)
                val (label, _) = som.getLabel(WorkerActor.toIMUVector(row))
                if
                //label.
              }))
          } catch {
            case e: NoSuchElementException =>
          }
        //TODO working here
        //TODO add other task type
        case msg => showError(s"unsupported message: $msg")
      }
      Log.info(s"finish task ${task.id}")
      DatabaseHelper.finishTask(task.id)
      Dispatcher.proxy ! TaskCompleted(task.id)
    case ReBindDispatcher => preStart()
      Log info "rebind dispatcher"
    case msg => showError(s"Unsupported msg : $msg")
  }

  override def preStart = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId, workerId)
  }

  val workerId: String = DatabaseHelper.clusterSeedId + "@" + self.path.toStringWithAddress(self.path.address) + "_" + System.nanoTime()
}
