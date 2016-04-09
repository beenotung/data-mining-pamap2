import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import scala.collection.JavaConverters._

/**
  * Created by beenotung on 1/24/16.
  */
object DatabaseHelperTest extends App {
  //val result: Object = DatabaseHelper.createDatabaseIfNotExist("testingNewDatabase")
  //val result = DatabaseHelper.resetTables("a", "b")
  //val result = DatabaseHelper.hasInit
  //val result = DatabaseHelper.numberOfServer
  //val result = DatabaseHelper.selectServerIdsByTag("public")
  //val result = DatabaseHelper.maxReplicas
  //DatabaseHelper.leaveReplicas
  //  val result = DatabaseHelper.findClusterSeedIds
  val fs = Tables.RawData.Field
  //  val result = DatabaseHelper.run(r=>r.table(Tables.RawData.name).sample(1)
  //      .getField(fs.timeSequence.toString)
  //      .map(reqlFunction1(row=>row.bracket()))
  //
  val f = Tables.IMU.Field.temperature.toString
  var c = 0
  try {
    val cursor = DatabaseHelper.getIMU(fs.hand.toString,1000L,fs.isTrain.toString)
      .foreach(row => {
        c += 1
//        println("temp:" + row.get(f))
        println(s"row:$row")
      })
  } catch {
    case e: NoSuchElementException =>
  }
  //  cursor.forEachRemaining(consumer(row=>{
  //  }))
  println(s"c:$c")
  //  println(s"result = $result")
  //  println(s"class = ${result.getClass}")
}
