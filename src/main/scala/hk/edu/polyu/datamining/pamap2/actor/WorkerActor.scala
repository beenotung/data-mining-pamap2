package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.actor.SingletonActor.Dispatcher
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import java.{util => ju}

import SequentialAR.{ItemSets, ItemSets_, Sequence, Sequence_}
import com.rethinkdb.RethinkDB
import com.rethinkdb.model.MapObject

import scala.collection.JavaConverters._
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.som.{Som, Vector}
import hk.edu.polyu.datamining.pamap2.utils.{Lang_, Log}

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
    case TaskAssign(taskId, task) =>
      task.id = taskId
      Log.info(s"received task id: ${task.id}, $task")
      val temperature_f: String = Tables.IMU.Field.temperature.toString
      task match {
        case ImuSomTrainingTask(label, trainingDataCount) =>
          Log.info(s"start training som for IMU part : $label")
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
              val xs = DatabaseHelper.getIMUPart(label, DatabaseHelper.BestInsertCount, Tables.RawData.Field.isTrain.toString)
              assert(xs != null, s"no sample for som IMU $label!")
              xs.takeWhile(_ => change > minChange)
                .foreach(map =>
                  change = som.addSample(Array(
                    map.get(x),
                    map.get(y),
                    map.get(z)
                  )))
              Log.debug(s"som for IMU-$label t:${som.t}, change:$change")
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
              DatabaseHelper.runToBuffer[AnyRef](r => r.table(Tables.RawData.name)
                .filter(r.hashMap(Tables.RawData.Field.isTrain.toString, true))
                .getField(fs.timeSequence.toString)
                .concatMap(reqlFunction1(row =>
                  //row.getField(fs.hand.toString).getField(f)
                  //.add(row.getField(fs.ankle.toString).getField(f))
                  //.add(row.getField(fs.chest.toString).getField(f))
                  row.getField(fs.chest.toString)
                    //.getField(f)
                    .pluck(f)
                )))
                .takeWhile(_ => change > minChange)
                .foreach(row =>
                  try {
                    val temp: Object = row.asInstanceOf[ju.Map[String, AnyRef]].get(f)
                    change = som.addSample(Array(temp.toString.toDouble))
                  } catch {
                    case e: NullPointerException => // timestamp without temperature
                  }
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
              DatabaseHelper.runToBuffer[AnyVal](r => r.table(Tables.RawData.name)
                .filter(r.hashMap(Tables.RawData.Field.isTrain.toString, true))
                .getField(fs.timeSequence.toString)
                .concatMap(reqlFunction1(row => row.getField(f)))
              ).takeWhile(_ => change > minChange)
                .foreach(temp =>
                  change = som.addSample(Array(temp.toString.toDouble))
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
        case MapRawDataToItemTask(imuIds, offset) =>
          /* reload som if not valid */
          if (!imuIds.equals(lastIMUIds)) {
            lastIMUIds = imuIds
            DatabaseHelper.runToBuffer[ju.Map[String, AnyRef]](_.table(Tables.SomImage.name))
              .foreach(map => {
                val som = Som.fromMap(map).get
                val label = som.labelPrefix
                somMap.update(label, som)
              })
          }
          /*
          * 1. get target raw data entry
          * 2. map to string lables
          * 3. save to database
          * */
          /*  1. get target raw data entry  */
          import Tables.RawData.{Field => fs}
          DatabaseHelper.runToBuffer[ju.Map[String, AnyRef]](_.table(Tables.RawData.name)
            .filter(fs.isTrain.toString, true)
            .skip(offset)
            .limit(1)
            //.nth(0) //optional
          ).foreach(activity => {
            import Tables.ActivityItemSetSequence
            val id = activity.get(DatabaseHelper.id).toString
            val activityTypeId = activity.get(fs.activityId.toString).toString
            val itemSetSequence: mutable.Buffer[Set[String]] = mutable.Buffer.empty
            val commonItemSet = mutable.Set.empty[String]

            /*  2. map to string labels  */

            /* 2.1 get comman labels */
            /* get subject data if not cached */
            val subjectId = activity.get(fs.subject.toString).toString
            if (subjectMap.get(subjectId).isEmpty)
              subjectMap.put(subjectId, DatabaseHelper.loadSubject(subjectId))
            val subject = subjectMap.get(subjectId).get
            import Tables.Subject.{Field => fs2}
            val weight_f = fs2.weight.toString
            val height_f = fs2.height.toString
            val age_f = fs2.age.toString
            commonItemSet += somMap.get(weight_f).get.getLabel(Array(subject.get(weight_f).toString.toDouble))._1
            commonItemSet += somMap.get(height_f).get.getLabel(Array(subject.get(height_f).toString.toDouble))._1
            commonItemSet += somMap.get(age_f).get.getLabel(Array(subject.get(age_f).toString.toDouble))._1
            try {
              val heartRate = activity.get(fs.heartRate.toString).toString.toDouble
              commonItemSet += somMap.get(fs.heartRate.toString).get.getLabel(Array(heartRate))._1
            } catch {
              case e: NullPointerException =>
              case e: NumberFormatException =>
              // no heart rate for this record
            }

            /* 2.2 get labels for each timesequence */
            activity.get(fs.timeSequence.toString).asInstanceOf[ju.List[ju.Map[String, AnyRef]]].asScala.foreach(row => {
              val itemSet = mutable.Set.empty[String]
              /* map temperature label */
              try {
                val temperature = row.get(fs.chest.toString).asInstanceOf[ju.Map[String, AnyRef]].get(temperature_f).toString.toDouble
                itemSet += somMap.get(temperature_f).get.getLabel(Array(temperature))._1
              } catch {
                case e: NullPointerException =>
                case e: NumberFormatException =>
                // no temperature in this row
              }
              /* map imu parts label */
              itemSet ++= Seq(fs.hand.toString, fs.ankle.toString, fs.chest.toString)
                .flatMap(bodyPart => ImuSomTrainingTask.values.map(_.toString)
                  .map(label => {
                    val x = label + "x"
                    val y = label + "y"
                    val z = label + "z"
                    val imupart = row.get(bodyPart).asInstanceOf[ju.Map[String, AnyVal]]
                    try {
                      val vector = Array(
                        imupart.get(x).toString.toDouble,
                        imupart.get(y).toString.toDouble,
                        imupart.get(z).toString.toDouble
                      )
                      Some(somMap.get(label).get.getLabel(vector)._1)
                    } catch {
                      // missing imu part in this row
                      case e: NullPointerException => None
                      case e: NumberFormatException => None
                    }
                  })
                  .filter(_.isDefined).map(_.get)
                )

              /* 2.3 add common labels into every row */
              itemSet ++= commonItemSet

              itemSetSequence += itemSet.toSet
            })

            /*  3. save to database  */
            val itemSetSequence_j: ju.List[ju.List[String]] = itemSetSequence.map(_.toList.asJava).asJava
            import Tables.ActivityItemSetSequence
            val row = RethinkDB.r.hashMap()
              .`with`(ActivityItemSetSequence.RawDataId, id)
              .`with`(ActivityItemSetSequence.ActivityTypeId, activityTypeId)
              .`with`(ActivityItemSetSequence.ItemSetSequence, itemSetSequence_j)
            DatabaseHelper.tableInsertRow(Tables.ActivityItemSetSequence.name, row)
          })
        case FirstSequenceGenerationTask(activityOffset) =>
          Log info s"generate first sequence item set, offset:$activityOffset"
          import Tables.ActivityItemSetSequence
          Log debug s"loading activity from server"
          val activitySeq = DatabaseHelper.runToBuffer[ju.List[String]](r => r.table(ActivityItemSetSequence.name)
            .skip(activityOffset)
            .getField(ActivityItemSetSequence.ItemSetSequence)
          )
            //            .map(list => new ItemSets_(IndexedSeq(list.asScala.toIndexedSeq)))
            .map(list => new ItemSets(Array(Lang_.toStringArray(list))))
          Log debug s"generating first sequence"
          //          val one_seq_sets = Sequence_.createFirstSeq(activitySeq)
          val one_seq_sets = Sequence.createFirstSeq(activitySeq.toArray)
          import Tables.OneSeqTemp
          val row = RethinkDB.r.hashMap(OneSeqTemp.PartId, activityOffset)
            .`with`(OneSeqTemp.OneSeqSets, one_seq_sets)
          Log debug s"saving the first sequence to database"
          DatabaseHelper.tableInsertRow(OneSeqTemp.name, row)
        //TODO working here
        case SequenceGenerationTask(activityOffset, seqOffset, seqCount) =>
          val activitySeq = DatabaseHelper.getTableRowMapObject(Tables.ActivityItemSetSequence, activityOffset)
          val activitySeqArr = ???
          val itemsetsSeq = new ItemSets(activitySeqArr)
          val sequenceSet: Array[Array[String]] = ???
          if (itemsetsSeq.isContain(sequenceSet)) {
            //TODO  add record +1 in database
          }

        //TODO add other task type
        case task: Task => showError(s"worker: unsupported task $task")
          fork(() => ???)
        case msg => showError(s"worker: unsupported message: $msg")
          fork(() => ???)
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
    log info "self register worker"
    SingletonActor.Dispatcher.proxy ! MessageProtocol.RegisterWorker(DatabaseHelper.clusterSeedId, workerId)
  }

  val workerId: String = DatabaseHelper.clusterSeedId + "@" + self.path.toStringWithAddress(self.path.address) + "_" + System.nanoTime()
}
