import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper

/**
  * Created by beenotung on 1/24/16.
  */
object DatabaseHelperTest extends App {
  //  val result: Object = DatabaseHelper.createDatabaseIfNotExist("testingNewDatabase")
  val result = DatabaseHelper.hasInit
  println(s"result = $result")
  println(s"class = ${result.getClass}")
}
