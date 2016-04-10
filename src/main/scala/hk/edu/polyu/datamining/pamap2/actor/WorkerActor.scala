package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import java.{util => ju}

import com.rethinkdb.RethinkDB
import com.rethinkdb.model.MapObject

import scala.collection.JavaConverters._
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.som.{Som, Vector}
import hk.edu.polyu.datamining.pamap2.utils.Log

import scala.collection.mutable


/**
  * Created by beenotung on 2/18/16.
  */
object WorkerActor {
  lazy val IMUField = Tables.IMU.Field


  /* TODO handle number format error */
  @deprecated
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
  val somMap = mutable.Map.empty[String, Som]
  val subjectMap = mutable.Map.empty[String, ju.Map[String, AnyRef]]

  override def receive: Receive = {
    case task: Task =>
      log.info(s"received task id: ${task.id}, $task")
      val temperature_f: String = Tables.IMU.Field.temperature.toString
      task match {
        case ImuSomTrainingTask(label, trainingDataCount) =>
          Log.info(s"start training som for $label")
          val minChange = Main.config.getDouble("algorithm.som.imuPart.minChange")
          try {
            val som = new Som(Array(1), label.toString, Som.randomGrids(3,
              Main.config.getInt("algorithm.som.imuPart.gridWidth"),
              Main.config.getInt("algorithm.som.imuPart.gridHeight"),
              Main.config.getDouble("algorithm.som.imuPart.initMin"),
              Main.config.getDouble("algorithm.som.imuPart.initMax")
            ))
            var change = Double.MaxValue
            val x = label + "x"
            val y = label + "y"
            val z = label + "z"
            while (change > minChange) {
              DatabaseHelper.getIMUPart(label, DatabaseHelper.BestInsertCount, Tables.RawData.Field.isTrain.toString)
                .takeWhile(_ => change > minChange)
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
          Log.info(s"start training som for temperature")
          try {
            val fs = Tables.RawData.Field
            val f: String = temperature_f
            val minChange = Main.config.getDouble("algorithm.som.temperature.minChange")
            val som = new Som(Array(1), f, Som.randomGrids(1,
              Main.config.getInt("algorithm.som.temperature.gridWidth"),
              Main.config.getInt("algorithm.som.temperature.gridHeight"),
              Main.config.getDouble("algorithm.som.temperature.initMin"),
              Main.config.getDouble("algorithm.som.temperature.initMax")
            ))
            var change = Double.MaxValue
            while (change > minChange) {
              DatabaseHelper.run(r => r.table(Tables.RawData.name)
                .filter(r.hashMap(Tables.RawData.Field.isTrain, true))
                .getField(fs.timeSequence.toString)
                .concatMap(reqlFunction1(row =>
                  //row.getField(fs.hand.toString).getField(f)
                  //.add(row.getField(fs.ankle.toString).getField(f))
                  //.add(row.getField(fs.chest.toString).getField(f))
                  row.getField(fs.chest.toString).getField(f)
                ))).asInstanceOf[ju.List[Double]].asScala
                .takeWhile(_ => change > minChange)
                .foreach(temp =>
                  change = som.addSample(Array(temp))
                )
            }
            Log.info(s"finished building som for $f, saving to database")
            DatabaseHelper.saveSom(som)
            Log.info(s"som for $f saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case HeartRateSomTrainingTask(trainingDataCount) =>
          val f = Tables.RawData.Field.heartRate.toString
          Log.info(s"start training som for $f")
          try {
            val fs = Tables.RawData.Field
            val minChange = Main.config.getDouble("algorithm.som.heartRate.minChange")
            val som = new Som(Array(1), f, Som.randomGrids(1,
              Main.config.getInt("algorithm.som.heartRate.gridWidth"),
              Main.config.getInt("algorithm.som.heartRate.gridHeight"),
              Main.config.getDouble("algorithm.som.heartRate.initMin"),
              Main.config.getDouble("algorithm.som.heartRate.initMax")
            ))
            var change = Double.MaxValue
            while (change > minChange) {
              DatabaseHelper.runToBuffer[Double](r => r.table(Tables.RawData.name)
                .filter(r.hashMap(Tables.RawData.Field.isTrain.toString, true))
                .getField(fs.timeSequence.toString)
                .concatMap(reqlFunction1(row => row.getField(f)))
              ).takeWhile(_ => change > minChange)
                .foreach(temp =>
                  change = som.addSample(Array(temp))
                )
            }
            Log.info(s"finished building som for $f, saving to database")
            DatabaseHelper.saveSom(som)
            Log.info(s"som for $f saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case WeightSomTrainingTask() =>
          val f = Tables.Subject.Field.weight.toString
          Log.info(s"start training som for $f")
          try {
            val minChange = Main.config.getDouble("algorithm.som.weight.minChange")
            val som = new Som(Array(1), f, Som.randomGrids(1,
              Main.config.getInt("algorithm.som.weight.gridWidth"),
              Main.config.getInt("algorithm.som.weight.gridHeight"),
              Main.config.getDouble("algorithm.som.weight.initMin"),
              Main.config.getDouble("algorithm.som.weight.initMax")
            ))
            trainSimple1DSom(som, minChange, Tables.Subject.name.toString, f)
            Log.info(s"finished building som for $f, saving to database")
            DatabaseHelper.saveSom(som)
            Log.info(s"som for $f saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case HeightSomTrainingTask() =>
          val f = Tables.Subject.Field.height.toString
          Log.info(s"start training som for $f")
          try {
            val minChange = Main.config.getDouble("algorithm.som.height.minChange")
            val som = new Som(Array(1), f, Som.randomGrids(1,
              Main.config.getInt("algorithm.som.height.gridWidth"),
              Main.config.getInt("algorithm.som.height.gridHeight"),
              Main.config.getDouble("algorithm.som.height.initMin"),
              Main.config.getDouble("algorithm.som.height.initMax")
            ))
            trainSimple1DSom(som, minChange, Tables.Subject.name.toString, f)
            Log.info(s"finished building som for $f, saving to database")
            DatabaseHelper.saveSom(som)
            Log.info(s"som for $f saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case AgeSomTrainingTask() =>
          val f = Tables.Subject.Field.age.toString
          Log.info(s"start training som for $f")
          try {
            val minChange = Main.config.getDouble("algorithm.som.age.minChange")
            val som = new Som(Array(1), f, Som.randomGrids(1,
              Main.config.getInt("algorithm.som.age.gridWidth"),
              Main.config.getInt("algorithm.som.age.gridHeight"),
              Main.config.getDouble("algorithm.som.age.initMin"),
              Main.config.getDouble("algorithm.som.age.initMax")
            ))
            trainSimple1DSom(som, minChange, Tables.Subject.name.toString, f)
            Log.info(s"finished building som for $f, saving to database")
            DatabaseHelper.saveSom(som)
            Log.info(s"som for $f saved to database")
          } catch {
            case e: NoSuchElementException =>
          }
        case ItemExtractTask(imuIds, offset) =>
          val fs = Tables.RawData.Field
          try {
            DatabaseHelper.run(r => r.table(Tables.RawData.name)
              .filter(fs.isTrain.toString, true)
              .skip(offset)
              .limit(1)
            ).asInstanceOf[ju.List[MapObject]]
              .forEach(consumer(activity => {
                val item = RethinkDB.r.hashMap(Tables.ItemsetCount.Field.count.toString, -1)
                val commonLabels = mutable.Buffer.empty[String]

                /* reload som if not valid */
                if (lastIMUIds != imuIds) {
                  lastIMUIds = imuIds
                  DatabaseHelper.run(r => r.table(Tables.SomImage.name)).asInstanceOf[ju.List[ju.Map[String, AnyRef]]]
                    .forEach(consumer(row => {
                      val som = Som.fromMap(row).get
                      val label = row.get(Tables.SomImage.LabelPrefix).toString
                      somMap.update(label, som)
                    }))
                }

                /* get subject data if not exist */
                val subjectId: String = activity.get(fs.subject.toString).asInstanceOf
                if (subjectMap.get(subjectId).isEmpty)
                  subjectMap.put(subjectId, DatabaseHelper.loadSubject(subjectId))
                val subject = subjectMap.get(subjectId).get
                val subject_f = Tables.Subject.Field
                val weight_f = subject_f.weight.toString
                val height_f = subject_f.height.toString
                val age_f = subject_f.age.toString
                commonLabels += somMap.get(weight_f).get.getLabel(Array(subject.get(subject_f).toString.toDouble))._1
                commonLabels += somMap.get(height_f).get.getLabel(Array(subject.get(subject_f).toString.toDouble))._1
                commonLabels += somMap.get(age_f).get.getLabel(Array(subject.get(subject_f).toString.toDouble))._1

                val heartRate = activity.get(fs.heartRate.toString).toString.toDouble
                commonLabels += somMap.get(fs.heartRate.toString).get.getLabel(Array(heartRate))._1
                /* replace data by som label */
                val timeSequenceLabels = activity.get(fs.timeSequence.toString).asInstanceOf[ju.List[ju.Map[String, AnyRef]]].asScala.map(row => {
                  val temperature = row.get(fs.chest).asInstanceOf[ju.Map[String, AnyRef]].get(temperature_f).toString.toDouble
                  val bodyParts = Seq(fs.hand.toString, fs.ankle.toString, fs.chest.toString)
                  val labels = mutable.Buffer.empty[String]
                  bodyParts.flatMap(bodyPart => ImuSomTrainingTask.values.map(_.toString).map(label => {
                    val x = label + "x"
                    val y = label + "y"
                    val z = label + "z"
                    val imupart = row.get(bodyPart).asInstanceOf[ju.Map[String, AnyVal]]
                    val vector = Array(
                      imupart.get(x).toString.toDouble,
                      imupart.get(y).toString.toDouble,
                      imupart.get(z).toString.toDouble
                    )
                    labels += somMap.get(label).get.getLabel(vector)._1
                  }))
                  labels += somMap.get(temperature_f).get.getLabel(Array(temperature))._1
                  labels.asJava
                }).asJava
                val fs2 = Tables.ItemsetCount.Field
                item.`with`(fs2.timeSequence.toString, timeSequenceLabels)
                  .`with`(fs2.common.toString, commonLabels.asJava)
                DatabaseHelper.tableInsertRow(Tables.ItemsetCount.name, item)
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

  def trainSimple1DSom(som: Som, minChange: Double, tableName: String, fieldName: String) = {
    var change = Double.MaxValue
    DatabaseHelper.runToBuffer[AnyVal](r => r.table(tableName).getField(fieldName))
      .takeWhile(_ => change > minChange)
      .foreach(row => change = som.addSample(Array(row.toString.toDouble)))
  }

  override def preStart = {
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId, workerId)
  }

  val workerId: String = DatabaseHelper.clusterSeedId + "@" + self.path.toStringWithAddress(self.path.address) + "_" + System.nanoTime()
}
