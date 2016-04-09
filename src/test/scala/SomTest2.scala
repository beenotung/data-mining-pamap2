import hk.edu.polyu.datamining.pamap2.actor.WorkerActor
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.som.{Som, Vector}
import java.{util => ju}

import hk.edu.polyu.datamining.pamap2.database.Tables.IMU.{Field => IMUField}

import scala.util.Random

/**
  * Created by beenotung on 4/9/16.
  */
object SomTest2 extends App {
  def toIMUVector(map: ju.Map[String, AnyRef]): Option[Vector] = try {
    Some(Array[Double](
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
    )
  } catch {
    case e: NullPointerException => None
    case e: NumberFormatException => None
  }

  val size = 30
  //  val som=new Som(
  //    weights = Array(1,1,1),
  //    labelPrefix = "hand_x",
  //    grids = Som.randomGrids(3,size,size,-256,256)
  //  )
  //  val weights = Array[Double](2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1)
  val weights = Array[Double](0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0)
  val som = new Som(
    weights = weights,
    labelPrefix = "hand_x",
    initGrids = Som.randomGrids(weights.length, size, size, -64d, 64d),
    normMode = Som.NormType.sigmoid
  )
  var change = Double.MaxValue
  val fs = Tables.RawData.Field
  DatabaseHelper.getIMU("hand", 10000L, fs.isTrain.toString).foreach(row => {
    //    println(s"row=$row")
    if (change > 0.001) {
      toIMUVector(row) match {
        case Some(vector) =>
          change = som.addSample(vector)
          println(s"change:$change")
        case None =>
      }
    }
  })
  var sum = 0d
  var count = 0
  val xs = DatabaseHelper.getIMU("hand", 1000L, fs.isTest.toString)
    .foreach(row => {
      toIMUVector(row) match {
        case None =>
          None
        case Some(vector) =>
          val (label, distance) = som.getLabel(vector)
          sum += distance
          count += 1
        //          println(s"label: $label\ndistance:$distance")
      }
    })
  println(s"average : ${sum / count}")
  println(s"t: ${som.t}")
}
