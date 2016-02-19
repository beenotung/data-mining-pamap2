package hk.edu.polyu.datamining.pamap2.database

/**
  * Created by beenotung on 1/26/16.
  */

import java.{lang => jl, util => ju}

import com.rethinkdb.RethinkDB
import com.rethinkdb.ast.ReqlAst
import com.rethinkdb.gen.ast.{Json, ReqlExpr}
import com.rethinkdb.net.Cursor
import com.typesafe.config.ConfigFactory
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.database.Tables.RawDataFile
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.JavaConverters._

object DatabaseHelper {
  val BestInsertCount = 200
  val r = com.rethinkdb.RethinkDB.r
  private val config = ConfigFactory parseResources "database.conf"
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


  /*    util functions    */
  var clusterSeedId: String = null

  def createTableDropIfExistResult(tableName: String): ju.HashMap[String, AnyRef] = {
    r.do_(createTableDropIfExist(tableName)).run(conn)
  }

  def createTableDropIfExist(tableName: String): java.util.List[_] = {
    r.array(
      r.db(dbname).tableDrop(tableName),
      r.db(dbname).tableCreate(tableName)
    )
  }

  def tableInsertRows[A](table: String, rows: java.util.List[A]): ju.HashMap[String, AnyRef] = {
    r.table(Tables.RawData.name).insert(rows).run(DatabaseHelper.conn)
  }

  def removeSeed(id: String = clusterSeedId): ju.HashMap[String, AnyVal] = {
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

  def addSeed(host: String, port: Int, roles: ju.List[String], config: Json): String = {
    initTables()
    val tableName = Tables.ClusterSeed.name
    val hostField = Tables.ClusterSeed.Field.host.toString
    val portField = Tables.ClusterSeed.Field.port.toString
    val rolesField = Tables.ClusterSeed.Field.roles.toString
    val configField = Tables.ClusterSeed.Field.config.toString
    val result: ju.HashMap[String, AnyVal] = r.table(tableName).insert(
      r.hashMap(hostField, host)
        .`with`(portField, port)
        .`with`(rolesField, roles)
        .`with`(configField, config)
    ).run(conn)
    clusterSeedId = result.get("generated_keys").asInstanceOf[java.util.List[String]].get(0)
    clusterSeedId
  }

  /**
    * create database if not exist
    * create tables if not exist
    **/
  def initTables(): Unit = {
    createDatabaseIfNotExistResult(dbname)
    Tables.tableNames.foreach(tableName => createTableIfNotExistResult(tableName))
  }

  def createDatabaseIfNotExistResult(dbname: String): ju.HashMap[String, AnyRef] = {
    createDatabaseIfNotExist(dbname).run(conn)
  }

  def createDatabaseIfNotExist(dbname: String): ReqlExpr = {
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist => r.branch(
      dbExist,
      r.hashMap("created", 0),
      r.dbCreate(dbname)
    )))
  }

  def createTableIfNotExistResult(tableName: String): ju.HashMap[String, AnyVal] = createTableIfNotExist(tableName).run(conn)

  def createTableIfNotExist(tableName: String): ReqlExpr =
    r.tableList().contains(tableName)
      .do_(reqlFunction1(tableExist => r.branch(
        tableExist,
        r.hashMap("created", 0),
        r.tableCreate(tableName)
      )))

  def findSeeds: Seq[(String, Int)] = {
    initTables()
    val tableName = Tables.ClusterSeed.name
    val hostField = Tables.ClusterSeed.Field.host.toString
    val portField = Tables.ClusterSeed.Field.port.toString
    //val configField = Tables.ClusterSeed.Field.config.toString
    val result: Cursor[ju.Map[String, AnyRef]] = r.table(tableName)
      .withFields(hostField, portField)
      .run(conn)
    result.toList.asScala
      .map { row => (row.get(hostField).asInstanceOf[String], row.get(portField).asInstanceOf[Long].toInt) }
      .toIndexedSeq
  }

  def addRawDataFile(filename: String, lines: Iterable[String], fileType: FileType): ju.HashMap[String, AnyRef] = {
    val field = Tables.RawDataFile.Field
    val row = r.hashMap(field.filename.toString, filename)
      .`with`(field.lines.toString, lines)
    fileType match {
      case FileType.training => row.`with`(field.isTraining.toString, true)
      case FileType.testing => row.`with`(field.isTesting.toString, true)
    }
    r.table(RawDataFile.name).insert(row).run(conn)
  }

  def getRawDataFileIds(): ju.List[String] = {
    ???
  }

  def run[A](fun: RethinkDB => ReqlAst) = fun(r).run(conn)

  def debug(key: String, value: Json): ju.HashMap[String, AnyRef] = {
    val table = Tables.Debug.name
    r.table(table).insert(r.hashMap(key, value)).run(conn)
  }

  def debug(key: String, value: String): ju.HashMap[String, AnyRef] = {
    val table = Tables.Debug.name
    r.table(table).insert(r.hashMap(key, value)).run(conn)
  }
}
