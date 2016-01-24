package hk.edu.polyu.datamining.pamap2.database

import java.util

import com.rethinkdb.RethinkDB.r
import com.typesafe.config.ConfigFactory

/**
  * Created by beenotung on 1/24/16.
  */
object DatabaseHelper {
  val config = ConfigFactory parseResources "database.conf"
  val hostname = config getString "rethinkdb.host"
  val port = config getInt "rethinkdb.port"
  val dbname = config getString "rethinkdb.dbname"
  val conn = r.connection()
    .hostname(hostname)
    .port(port)
    .db(dbname)
    .connect()

  def createDatabase(dbname: String) = {
    val result: util.HashMap[String, Object] = r.dbList().contains(dbname)
      .do_((exist: Boolean) => r.branch(
        exist,
        "{created:0}",
        r.dbCreate(dbname)
      ))
      .run(conn)
    println(s"result : $result")
  }
}
