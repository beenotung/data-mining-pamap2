package hk.edu.polyu.datamining.pamap2.database

/**
  * Created by beenotung on 1/26/16.
  */

import java.time.OffsetDateTime
import java.{lang => jl, util => ju}

import com.rethinkdb.RethinkDB
import com.rethinkdb.ast.ReqlAst
import com.rethinkdb.gen.ast.{Json, ReqlExpr}
import com.rethinkdb.gen.exc.ReqlDriverError
import com.rethinkdb.model.OptArgs
import com.rethinkdb.net.{Connection, Cursor}
import com.typesafe.config.ConfigFactory
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.Task
import hk.edu.polyu.datamining.pamap2.database.Tables.{RawDataFile, Task}
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import hk.edu.polyu.datamining.pamap2.utils.{Lang, Lang_, Log}

import scala.collection.JavaConverters._

object DatabaseHelper {
  /* db constants */
  lazy val rethinkdb = "rethinkdb"
  lazy val server_config = "server_config"
  lazy val tags = "tags"
  lazy val id = "id"
  lazy val shards = "shards"
  lazy val replicas = "replicas"
  lazy val BestInsertCount = 200
  lazy val durability = "durability"
  lazy val soft = "soft"
  lazy val hard = "hard"
  lazy val new_val = "new_val"
  lazy val old_val = "old_val"
  lazy val include_initial = "include_initial"

  /* self defined db constants */
  lazy val value = "value"

  /* db variables */
  val r = com.rethinkdb.RethinkDB.r
  val generated_keys: String = "generated_keys"
  private val config = ConfigFactory parseResources "database.conf"
  private val port = config getInt "rethinkdb.port"
  private val dbname = config getString "rethinkdb.dbname"
  private val (conn, isUsingBackupHost) = {
    val (conn, isUsingBackupHost) = try {
      val hostname = config getString "rethinkdb.host"
      println(s"Database : try to connect to $hostname")
      val conn = r.connection()
        .hostname(hostname)
        .port(port)
        //.db(dbname)
        .connect()
      (conn, false)
    }
    catch {
      case e: Exception =>
        System.err.println(e)
        val hostname = config getString "rethinkdb.backup_host"
        println(s"Database : try to connect to $hostname")
        val conn = r.connection()
          .hostname(hostname)
          .port(port)
          //.db(dbname)
          .connect()
        (conn, true)
    }
    initTables(conn)
    conn.use(dbname)
    if (!isUsingBackupHost)
      maxReplicas(conn)
    println(s"Database : connected to ${conn.hostname}")
    (conn, isUsingBackupHost)
  }
  DatabaseHelper_.setConn(conn)

  var clusterSeedId: String = null

  def tableNames: ju.List[String] = r.db(dbname).tableList().run(conn)

  def selectServerIdsByTag(tag: String) = {
    run[Cursor[List[String]]](r => r.db(rethinkdb).table(server_config)
      .filter(reqlFunction1(server => server.getField(tags).contains(tag)))
      .map(reqlFunction1(server => server.getField(id)))
    ).toList
  }

  def maxReplicas(conn: Connection = conn) = updateReplicas(conn, 0)

  def leaveReplicas(conn: Connection = conn) = if (isUsingBackupHost) updateReplicas(conn, 1)

  def updateReplicas(conn: Connection = conn, offset: Long): ju.HashMap[String, AnyRef] = {
    //initTables(conn)
    val n = numberOfServer(conn) - offset
    r.db(dbname).tableList().forEach(reqlFunction1(table => r.db(dbname).table(table)
      .reconfigure()
      .optArg(shards, 1)
      .optArg(replicas, n)
    )).run(conn)
  }

  /*    util functions    */
  def numberOfServer(conn: Connection = conn): Long = r.db("rethinkdb").table("server_config").count().run(conn)

  def createTableDropIfExistResult(tableName: String) = {
    run(_.db(dbname).tableDrop(tableName)).asInstanceOf[ju.Map[String, AnyRef]]
    run(_.db(dbname).tableCreate(tableName)).asInstanceOf[ju.Map[String, AnyRef]]
  }

  def tableInsertRows[A](table: String, rows: java.util.List[A], softDurability: Boolean = false): ju.HashMap[String, AnyRef] = conn.synchronized {
    try {
      r.table(table).insert(rows).run(conn, OptArgs.of(durability, if (softDurability) soft else hard))
    } catch {
      case e: ReqlDriverError =>
        Log.error("try to reconnect database", e)
        conn.reconnect()
        r.table(table).insert(rows).run(conn, OptArgs.of(durability, if (softDurability) soft else hard))
    }
  }

  def tableUpdate(tableName: String, idValue: String, value: com.rethinkdb.model.MapObject): ju.HashMap[String, AnyRef] =
    run(_.table(tableName).get(idValue).update(value))

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

  def getActionStatus: ActionStatusType = {
    val value: Any = getValue(
      tablename = Tables.Status.name,
      idValue = ActionStatus.name,
      defaultValue = ActionStatus.init.toString
    ).run(conn)
    val actionStatus = ActionStatus.withName(value.toString)
    actionStatus
  }

  def getValue[A](dbname: String = dbname, tablename: String, idValue: String, fieldname: String = value, defaultValue: A): ReqlAst =
    r.branch(
      // db exist
      r.dbList().contains(dbname),
      r.branch(
        // table exist
        r.db(dbname).tableList().contains(tablename),
        r.branch(
          // record exist
          r.db(dbname).table(tablename).contains(reqlFunction1(_.bracket(id).eq(idValue))),
          r.db(dbname).table(tablename).get(idValue).bracket(fieldname),
          r expr defaultValue
        ),
        r expr defaultValue
      ),
      r expr defaultValue
    )

  def listenValue[A](dbname: String = dbname, tablename: String, idValue: String, fieldname: String = value, includeInitial: Boolean = false, callback: A => Unit) = {
    val cursor: Cursor[ju.Map[String, ju.Map[String, AnyRef]]] = r.db(dbname).table(tablename).get(idValue).changes().optArg(include_initial, includeInitial).run(conn)
    cursor.forEach(Lang.consumer(feed => {
      callback(feed.get(new_val).get(fieldname).asInstanceOf[A])
    }))
    cursor
  }

  def setActionStatus(actionStatusType: ActionStatusType) = setValue(
    tablename = Tables.Status.name,
    idValue = ActionStatus.name,
    newVal = actionStatusType.toString
  )

  def setValue[A](dbname: String = dbname, tablename: String, idValue: String, fieldname: String = value, newVal: A): ju.HashMap[String, AnyRef] = conn.synchronized {
    createDatabaseIfNotExistResult(dbname)
    createTableIfNotExistResult(tablename)
    run(r => {
      println(s"set value of $idValue in table $tablename $fieldname:$newVal")
      r.table(tablename).get(idValue).replace(r.hashMap(id, idValue).`with`(fieldname, newVal))
    })
  }

  def createDatabaseIfNotExistResult(dbname: String): ju.HashMap[String, AnyRef] = {
    run(_ => createDatabaseIfNotExist(dbname))
  }

  def addSeed(host: String, port: Int, roles: ju.List[String], config: Json): String = {
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

    /** REMARK : this assignment cannot be removed */
    clusterSeedId = result.get("generated_keys").asInstanceOf[java.util.List[String]].get(0)
    clusterSeedId
  }

  def findSeeds: Seq[(String, Int)] = {
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

  def findClusterSeedIds = {
    run[Cursor[ju.Map[String, String]]](r => r.table(Tables.ClusterSeed.name).withFields(id))
      .iterator().asScala.map(_.get(id)).toIndexedSeq
  }

  /**
    * create database if not exist
    * create tables if not exist
    **/
  def initTables(conn: Connection = conn): Unit = {
    createDatabaseIfNotExist(dbname).run(conn)
    conn.use(dbname)
    Tables.tableNames.foreach(tableName => {
      println(s"checking table : $tableName")
      createTableIfNotExistResult(tableName, conn)
    })
    println("check table : finished")
  }

  def createDatabaseIfNotExist(dbname: String): ReqlExpr = {
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist => r.branch(
      dbExist,
      r.hashMap("created", 0),
      r.dbCreate(dbname)
    )))
  }

  def createTableIfNotExistResult(tableName: String, conn: Connection = conn): ju.HashMap[String, AnyVal] = createTableIfNotExist(tableName).run(conn)

  def createTableIfNotExist(tableName: String, dbname: String = dbname): ReqlExpr =
    r.tableList().contains(tableName)
      .do_(reqlFunction1(tableExist => r.branch(
        tableExist,
        r.hashMap("created", 0),
        r.tableCreate(tableName)
      )))

  /** this approach generate large network traffic demand */
  @deprecated
  def addRawDataFile(filename: String, lines: ju.List[String], fileType: FileType): ju.HashMap[String, AnyRef] = {
    val field = Tables.RawDataFile.Field
    val row = r.hashMap(field.filename.toString, filename)
      .`with`(field.lines.toString, lines)
    fileType match {
      case FileType.training => row.`with`(field.isTraining.toString, true)
      case FileType.testing => row.`with`(field.isTesting.toString, true)
    }
    r.table(RawDataFile.name).insert(row).run(conn)
  }

  def addNewTask(task: Task, clusterId: String, workerId: String, pending: Boolean = false): String = {
    val field = Tables.Task.Field
    //TODO add more detail about the task
    val row = r.hashMap(field.taskType.toString, task.getClass.getName)
      .`with`(field.workerId.toString, workerId)
      .`with`(field.clusterId.toString, clusterId)
      .`with`(field.createTime.toString, OffsetDateTime.now())
    if (pending)
      row.`with`(field.pending.toString, true)
    run[ju.List[String]](r => r.table(Tables.Task.name).insert(row)
      .getField(generated_keys))
      .get(0)
  }

  def reassignTask(taskId: String, clusterId: String, workerId: String) = {
    val field = Tables.Task.Field
    run[Any](r => r.table(Tables.Task.name).get(taskId).update(
      r.hashMap(field.workerId.toString, workerId)
        .`with`(field.clusterId.toString, clusterId)
        .`with`(field.createTime.toString, OffsetDateTime.now())
    ))
  }

  def getTasksByWorkerId(workerId: String): Seq[Task] = {
    //TODO
    Seq.empty
  }

  def finishTask(taskId: String): ju.HashMap[String, AnyRef] =
    run(r => r.table(Task.name).get(taskId).update(reqlFunction1(row => r.hashMap(Task.Field.completeTime.toString, OffsetDateTime.now()))))

  def run[A](fun: RethinkDB => ReqlAst): A = conn.synchronized({
    try {
      assert(r != null)
      assert(fun != null)
      assert(conn != null)
      fun(r).run(conn)
    } catch {
      case e: ReqlDriverError =>
        fork(runnable(() => {
          throw e
        }))
        Log.error("try to reconnect database", e)
        conn.reconnect()
        fun(r).run(conn)
    }
  })

  /* for java */
  def run_[A](fun: Lang_.ProducerConsumer[RethinkDB, ReqlAst]): A = fun.apply(r).run(conn)

  def connSync[A](f: Lang_.Producer[A]) = conn.synchronized(f.apply())

  def debug(key: String, value: Json): ju.HashMap[String, AnyRef] = {
    val table = Tables.Debug.name
    r.table(table).insert(r.hashMap(key, value)).run(conn)
  }

  def debug(key: String, value: String): ju.HashMap[String, AnyRef] = {
    val table = Tables.Debug.name
    r.table(table).insert(r.hashMap(key, value)).run(conn)
  }

  def getHostInfo(clusterSeedId: String) = {
    try {
      val field = Tables.ClusterSeed.Field
      val res = run[ju.HashMap[String, AnyRef]](r => r.db(dbname).table(Tables.ClusterSeed.name)
        .get(clusterSeedId)
        .without(field.config.toString)
      )
      (res.get(field.host.toString).asInstanceOf[String],
        res.get(field.port.toString).asInstanceOf[Long],
        res.get(field.roles.toString).asInstanceOf[ju.List[String]])
    } catch {
      case e: Exception => ("removed", -1L, new ju.ArrayList[String]())
    }
  }

  def markTrainSample(percentage: Double) =
    DatabaseHelper_.markTrainSample_(Tables.RawData.name, Tables.RawData.Field.isTrain.toString, percentage)
}
