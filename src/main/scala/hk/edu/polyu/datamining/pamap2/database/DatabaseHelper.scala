package hk.edu.polyu.datamining.pamap2.database

/**
  * Created by beenotung on 1/26/16.
  */

import java.util

import com.rethinkdb.gen.ast.{Json, ReqlExpr}
import com.typesafe.config.ConfigFactory
import hk.edu.polyu.datamining.pamap2.utils.Lang._

object DatabaseHelper {
  val BestInsertCount = 200
  val r = com.rethinkdb.RethinkDB.r
  private val config = ConfigFactory parseResources "database.conf"
  //  private val hostname = config getString "rethinkdb.host"
  private val port = config getInt "rethinkdb.port"
  private val dbname = config getString "rethinkdb.dbname"
  private val conn = {
    val conn = try {
      val hostname = config getString "rethinkdb.host"
      println(s"Database : try to connect to $hostname")
      r.connection()
        .hostname(hostname)
        .port(port)
        .db(dbname)
        .connect()
    }
    catch {
      case e: Exception =>
        val hostname = config getString "rethinkdb.backup_host"
        println(s"Database : try to connect to $hostname")
        r.connection()
          .hostname(hostname)
          .port(port)
          .db(dbname)
          .connect()
    }
    println(s"Database : connected to ${conn.hostname}")
    conn
  }

  def createDatabaseIfNotExist(dbname: String): ReqlExpr = {
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist => r.branch(
      dbExist,
      r.hashMap("created", 0),
      r.dbCreate(dbname)
    )))
  }

  def createDatabaseIfNotExistResult(dbname: String): util.HashMap[String, AnyRef] = {
    createDatabaseIfNotExist(dbname).run(conn)
  }

  def createTableDropIfExist(tableName: String): java.util.List[_] = {
    r.array(
      r.db(dbname).tableDrop(tableName),
      r.db(dbname).tableCreate(tableName)
    )
  }

  def createTableDropIfExistResult(tableName: String): util.HashMap[String, AnyRef] = {
    r.do_(createTableDropIfExist(tableName)).run(conn)
  }

  def createTableIfNotExist(tableName: String): ReqlExpr =
    r.tableList().contains(tableName)
      .do_(reqlFunction1(tableExist => r.branch(
        tableExist,
        r.hashMap("created", 0),
        r.tableCreate(tableName)
      )))

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

  /**
    * create database if not exist
    * create tables if not exist
    **/
  def initTables(): Unit = {
    val queries = new util.Vector[ReqlExpr]
    queries add createDatabaseIfNotExist(dbname)
    Tables.tableNames.foreach(tableName => queries add createTableIfNotExist(tableName))
    r.expr(queries).run(conn)
  }

  def addSeed(host: java.util.List[String], port: Int, config: Json): util.HashMap[String, AnyVal] = {
    initTables()
    val tableName = Tables.ClusterSeed.name
    val hostField = Tables.ClusterSeed.Field.host.toString
    val portField = Tables.ClusterSeed.Field.port.toString
    val configField = Tables.ClusterSeed.Field.config.toString
    r.table(tableName).insert(
      r.hashMap(hostField, host)
        .`with`(portField, port)
        .`with`(configField, config)
    ).run(conn)
  }

  def removeSeed(host: String, port: Int): util.HashMap[String, AnyVal] = {
    val tableName = Tables.ClusterSeed.name
    val hostField = Tables.ClusterSeed.Field.host.toString
    val portField = Tables.ClusterSeed.Field.port.toString
    r.table(tableName)
      .filter(
        r.hashMap(hostField, host)
          .`with`(portField, port)
      )
      .delete()
      .run(conn)
  }

  def removeSeed(id: String): util.HashMap[String, AnyVal] = {
    val tableName = Tables.ClusterSeed.name
    r.table(tableName)
      .get(id)
      .delete()
      .run(conn)
  }

  /**
    * REMOVE then create database
    **/
  def resetTables(currentStatus: String, nextStatus: String): Unit = {
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

  def insert[A](table: String, rows: java.util.List[A]): util.HashMap[String, AnyRef] = {
    r.table(Tables.RawData.name).insert(rows).run(DatabaseHelper.conn)
  }
}
