package hk.edu.polyu.datamining.pamap2.actor

import com.rethinkdb.RethinkDB.r
import com.rethinkdb.model.MapObject
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.database.Tables
import hk.edu.polyu.datamining.pamap2.utils.FormatUtils._

import scala.language.postfixOps

/**
  * Created by beenotung on 1/21/16.
  */
object ImportActor {

  lazy val IMUField = Tables.IMU.Field
  lazy val RawField = Tables.RawData.Field
  var lineOffset = 0

  def processSubject(titles: IndexedSeq[String], line: String): MapObject = {
    val map = r.hashMap()
    val cols = line.split(",")
    titles.indices.foreach(col => {
      map.`with`(titles(col), cols(col))
    })
    map
  }

  def processLine(line: String): MapObject = {
    //lineOffset += 1
    //println(s"processing line : $lineOffset")
    val cols = line.split(" ")
    r.hashMap(RawField.timestamp.toString, toFloat(cols(0)))
      .`with`(RawField.activityId.toString, toByte(cols(1)))
      .`with`(RawField.heartRate.toString, toShort(cols(2)))
      .`with`(RawField.hand.toString, processIMU(cols, 3))
      .`with`(RawField.chest.toString, processIMU(cols, 20))
      .`with`(RawField.ankle.toString, processIMU(cols, 37))
  }

  def processIMU(cols: Array[String], offset: Int): MapObject = {
    r.hashMap(IMUField.temperature.toString, toFloat(cols(offset)))
      .`with`(IMUField.a16x.toString, toFloat(cols(offset + 1)))
      .`with`(IMUField.a16y.toString, toFloat(cols(offset + 2)))
      .`with`(IMUField.a16z.toString, toFloat(cols(offset + 3)))
      .`with`(IMUField.a6x.toString, toFloat(cols(offset + 4)))
      .`with`(IMUField.a6y.toString, toFloat(cols(offset + 5)))
      .`with`(IMUField.a6z.toString, toFloat(cols(offset + 6)))
      .`with`(IMUField.rx.toString, toFloat(cols(offset + 7)))
      .`with`(IMUField.ry.toString, toFloat(cols(offset + 8)))
      .`with`(IMUField.rz.toString, toFloat(cols(offset + 9)))
      .`with`(IMUField.mx.toString, toFloat(cols(offset + 10)))
      .`with`(IMUField.my.toString, toFloat(cols(offset + 11)))
      .`with`(IMUField.mz.toString, toFloat(cols(offset + 12)))
  }

  case class ImportFile(fileType: FileType, filename: String, lines: Seq[String])

  final case class HandleLines(filename: String, lineOffset: Int, lineCount: Int, lines: Iterable[String])

  object FileType extends Enumeration {
    type FileType = Value
    val subject, training, testing = Value
  }

}
