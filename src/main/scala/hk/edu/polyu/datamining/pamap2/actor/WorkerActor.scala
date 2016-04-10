package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import java.{util => ju}

import com.rethinkdb.model.MapObject

import scala.collection.JavaConverters._
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.WorkerActor._
import hk.edu.polyu.datamining.pamap2.som.{Som, Vector}
import hk.edu.polyu.datamining.pamap2.utils.Log

import scala.util.Random

/**
  * Created by beenotung on 2/18/16.
  */
object WorkerActor {
  lazy val IMUField = Tables.IMU.Field

  lazy val IMUPartGridWidth = Main.config.getInt("algorithm.som.imuPart.gridWidth")
  lazy val IMUPartGridHeight = Main.config.getInt("algorithm.som.imuPart.gridHeight")
  lazy val IMUPartSomMinChange = Main.config.getInt("algorithm.som.imuPart.minChange")

  lazy val HeartRateGridWidth = Main.config.getInt("algorithm.som.heartRate.gridWidth")
  lazy val HeartRateGridHeight = Main.config.getInt("algorithm.som.heartRate.gridHeight")
  lazy val HeartRateSomMinChange = Main.config.getInt("algorithm.som.heartRate.minChange")

  lazy val TemperatureGridWidth = Main.config.getInt("algorithm.som.temperature.gridWidth")
  lazy val TemperatureGridHeight = Main.config.getInt("algorithm.som.temperature.gridHeight")
  lazy val TemperatreSomMinChange = Main.config.getInt("algorithm.som.temperature.minChange")

  /* TODO handle number format error */
  def toIMUVector(map: ju.Map[String, Any]): Vector = {
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
  var handSom, ankleSom, chestSom, heartRateSom: Som = null

  override def receive: Receive = {
    case task: Task =>
      log.info(s"received task id: ${task.id}, $task")
      task match {
        case ImuSomTrainingTask(label, trainingDataCount) =>
          Log.info(s"start training som for $label")
          try {
            val som = new Som(
              weights = Array(1, 1, 1),
              labelPrefix = label.toString,
              initGrids = Som.randomGrids(3, IMUPartGridWidth, IMUPartGridHeight, -256d, 256d)
            )
            var change = Double.MaxValue
            val x = label + "x"
            val y = label + "y"
            val z = label + "z"
            while (change > IMUPartSomMinChange) {
              DatabaseHelper.getIMUPart(label, DatabaseHelper.BestInsertCount, Tables.RawData.Field.isTrain.toString)
                .takeWhile(_ => change > IMUPartSomMinChange)
                .foreach(map =>
                  change = som.addSample(Array(
                    map.get(x),
                    map.get(y),
                    map.get(z)
                  )))
            }
            Log.info(s"finished building som for $label, saving to database")
            DatabaseHelper.saveSom(som)
            Log.info(s"som for $label saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case TemperatureSomTrainingTask(trainingDataCount) =>
        case HeartRateSomTrainingTask(trainingDataCount) =>
        case ItemCountTask(imuIds, offset) =>
          val fs = Tables.RawData.Field
          val imuFs = Tables.IMU.Field
          try {
            DatabaseHelper.run(r => r.table(Tables.RawData.name)
              .filter(fs.isTrain.toString, true)
              .skip(offset)
              .limit(1)
            ).asInstanceOf[ju.List[MapObject]]
              .forEach(consumer(row => {
                if (lastIMUIds != imuIds) {
                  DatabaseHelper.run(r => r.table(Tables.SomImage.name)).asInstanceOf[ju.List[ju.Map[String, AnyRef]]]
                    .forEach(consumer(row => row.get(Tables.SomImage.LabelPrefix) match {
                      case s: String if s.equals(fs.ankle.toString) => ankleSom = Som.fromMap(row).get
                      case s: String if s.equals(fs.hand.toString) => handSom = Som.fromMap(row).get
                      case s: String if s.equals(fs.chest.toString) => chestSom = Som.fromMap(row).get
                    }))
                }
                /* replace data by som label */
                //                val ankle = row.get(fs.ankle.toString).asInstanceOf[ju.Map[String, Any]]
                //                row.`with`(fs.ankle.toString, ankleSom.getLabel(toIMUVector(ankle)))
                //
                //                val hand = row.get(fs.hand.toString).asInstanceOf[ju.Map[String, Any]]
                //                row.`with`(fs.hand.toString, handSom.getLabel(toIMUVector(hand)))
                //
                //                val chest = row.get(fs.chest.toString).asInstanceOf[ju.Map[String, Any]]
                //                row.`with`(fs.chest.toString, chestSom.getLabel(toIMUVector(chest)))
                //
                //                val heartRate = row.get(fs.heartRate.toString)
                //                row.`with`(fs.heartRate.toString, heartRateSom.getLabel(toIMUVector(heartRate)))

                //                row.`with`(a)
                //                val (label, _) = som.getLabel(toIMUVector(row))
                //                if
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
