package hk.edu.polyu.datamining.pamap2.database

/**
  * Created by beenotung on 1/26/16.
  */

import java.util

import com.typesafe.config.ConfigFactory
import hk.edu.polyu.datamining.pamap2.utils.Lang._

object DatabaseHelper {
  val r = com.rethinkdb.RethinkDB.r
  private val config = ConfigFactory parseResources "database.conf"
  private val hostname = config getString "rethinkdb.host"
  private val port = config getInt "rethinkdb.port"
  private val dbname = config getString "rethinkdb.dbname"
  private val conn = r.connection()
      .hostname(hostname)
      .port(port)
      .db(dbname)
      .connect()

  def createDatabaseIfNotExist(dbname: String): util.HashMap[String, AnyRef] = {
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist => r.branch(
      dbExist,
      r.hashMap("created", 0),
      r.dbCreate(dbname)
    ))).run(conn)
  }

  def createTableDropIfExist(tableName: String): util.HashMap[String, AnyRef] = {
    r.db(dbname).table(tableName).delete.run(conn)
    r.db(dbname).tableCreate(tableName).run(conn)
  }

  def hasInit: Boolean = {
    val tableName: String = Tables.Status.name
    val fieldName: String = Tables.Status.Field.actionStatus.toString
    /* check if database exist */
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist =>
      r.branch(
        dbExist,
        /* check if table exist */
        r.db(dbname).tableList().contains(tableName).do_(reqlFunction1(tableExist => r.branch(
          tableExist,
          /* check if field exist */
          r.db(dbname).table(tableName).withFields(fieldName).count().ge(1),
          r expr false
        ))),
        r expr false
      ))
    ).run(conn)
  }

  def init(currentStatus: String, nextStatus: String): Unit = {
    val statusTableName: String = Tables.Status.name
    val statusFieldName: String = Tables.Status.Field.actionStatus.toString
    /* drop and create database */
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist => r.branch(
      dbExist,
      r.dbDrop(dbname),
      r.hashMap("dbs_dropped", 0)
    ))).run(conn)
    r.dbCreate(dbname).run(conn)
    conn.use(dbname)
    /* create status table */
    r.tableCreate(statusTableName).run(conn)
    /* save current status */
    r.table(statusTableName).insert(r.hashMap(statusFieldName, currentStatus)).run(conn)
    /* create other tables */
    Tables.tableNames
        .filterNot(statusTableName.equals)
        .foreach(t => unit(r.tableCreate(t).run(conn)))
    /* update status */
    r.table(statusTableName).update(r.hashMap(statusFieldName, nextStatus)).run(conn)
  }
}
