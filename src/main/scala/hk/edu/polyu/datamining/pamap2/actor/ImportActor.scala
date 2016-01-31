package hk.edu.polyu.datamining.pamap2.actor

import com.rethinkdb.RethinkDB.r
import hk.edu.polyu.datamining.pamap2.database.Tables

import scala.language.postfixOps

/**
  * Created by beenotung on 1/21/16.
  */
object ImportActor {

  lazy val IMUField = Tables.IMU.Field
  lazy val RawField = Tables.RawData.Field

  def processLine(line: String) = {
    val cols = line.split(" ")
    r.hashMap(RawField.timestamp.toString, cols(0).toFloat)
      .`with`(RawField.activityId.toString, cols(1).toByte)
      .`with`(RawField.heartRate.toString, cols(2).toShort)
      .`with`(RawField.hand.toString, processIMU(cols, 3))
      .`with`(RawField.chest.toString, processIMU(cols, 20))
      .`with`(RawField.ankle.toString, processIMU(cols, 37))
  }

  def processIMU(cols: Array[String], offset: Int) = {
    r.hashMap(IMUField.temperature.toString, cols(offset).toFloat)
      .`with`(IMUField.a16x.toString, cols(offset + 1).toFloat)
      .`with`(IMUField.a16y.toString, cols(offset + 2).toFloat)
      .`with`(IMUField.a16z.toString, cols(offset + 3).toFloat)
      .`with`(IMUField.a6x.toString, cols(offset + 4).toFloat)
      .`with`(IMUField.a6y.toString, cols(offset + 5).toFloat)
      .`with`(IMUField.a6z.toString, cols(offset + 6).toFloat)
      .`with`(IMUField.rx.toString, cols(offset + 7).toFloat)
      .`with`(IMUField.ry.toString, cols(offset + 8).toFloat)
      .`with`(IMUField.rz.toString, cols(offset + 9).toFloat)
      .`with`(IMUField.mx.toString, cols(offset + 10).toFloat)
      .`with`(IMUField.my.toString, cols(offset + 11).toFloat)
      .`with`(IMUField.mz.toString, cols(offset + 12).toFloat)
  }

  case class ImportFile(filename: String, lines: Seq[String])

  final case class HandleLines(filename: String, lineOffset: Int, lineCount: Int, lines: Iterable[String])

}
