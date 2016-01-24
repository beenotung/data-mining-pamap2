package hk.edu.polyu.datamining.pamap2.database

import com.rethinkdb.RethinkDB.r
import com.rethinkdb.gen.ast.Funcall
import com.typesafe.config.ConfigFactory

/**
  * Created by beenotung on 1/24/16.
  */
object DatabaseHelper {
  val config = ConfigFactory parseResources "database.conf"
  val hostname = config getString "rethinkdb.host"
  val port = config getInt "rethinkdb.port"
  val dbname = config getString "dbname"
  val conn = r.connection()
    .hostname(hostname)
    .port(port)
    .db(dbname)
    .connect()

  def createDatabase(dbname: String): Funcall = {
//    return r.dbList().contains(dbname)
//      .do_((exist: Boolean) => r.branch(
//        exist,
//        "{}",
//        r.dbCreate(dbname)
//      ))
  }
}
