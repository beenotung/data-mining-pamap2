package hk.edu.polyu.datamining.pamap2.database

/**
  * Created by beenotung on 1/26/16.
  */

import java.time.OffsetDateTime
import java.util.NoSuchElementException
import java.{lang => jl, util => ju}

import com.rethinkdb.RethinkDB
import com.rethinkdb.ast.ReqlAst
import com.rethinkdb.gen.ast.{Json, ReqlExpr}
import com.rethinkdb.gen.exc.ReqlDriverError
import com.rethinkdb.model.{MapObject, OptArgs}
import com.rethinkdb.net.{Connection, Cursor}
import com.sun.istack.internal.NotNull
import com.typesafe.config.ConfigFactory
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol._
import hk.edu.polyu.datamining.pamap2.database.Tables.{RawDataFile, Table, Task}
import hk.edu.polyu.datamining.pamap2.som.Som
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import hk.edu.polyu.datamining.pamap2.utils.{Lang, Lang_, Log}

import scala.collection.JavaConverters._
import scala.collection.mutable

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
  lazy val return_changes = "return_changes"

  /* self defined db constants */
  lazy val value = "value"

  lazy val Max_Row = 1600L

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
    //    if (!isUsingBackupHost)
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

  def updateReplicas(conn: Connection = conn, offset: Long): Option[ju.HashMap[String, AnyRef]] = {
    //initTables(conn)
    val n = numberOfServer(conn) - offset
    if (n >= 1)
      Some(r.db(dbname).tableList().forEach(reqlFunction1(table => r.db(dbname).table(table)
        .reconfigure()
        .optArg(shards, 1)
        .optArg(replicas, n)
      )).run(conn).asInstanceOf[ju.HashMap[String, AnyRef]])
    else
      None
  }

  /*    util functions    */
  def numberOfServer(conn: Connection = conn): Long = conn.synchronized(r.db("rethinkdb").table("server_config").count().run(conn))

  def createTableDropIfExistResult(tableName: String) = {
    run(_.db(dbname).tableDrop(tableName)).asInstanceOf[ju.Map[String, AnyRef]]
    run(_.db(dbname).tableCreate(tableName)).asInstanceOf[ju.Map[String, AnyRef]]
  }

  def tableInsertRow[A](table: String, row: A, softDurability: Boolean = false): ju.HashMap[String, AnyRef] = conn.synchronized {
    try {
      assert(row != null, "row cannot be null")
      r.table(table).insert(row).run(conn, OptArgs.of(durability, if (softDurability) soft else hard))
    } catch {
      case e: ReqlDriverError =>
        Log.error("try to reconnect database", e)
        conn.reconnect()
        r.table(table).insert(row).run(conn, OptArgs.of(durability, if (softDurability) soft else hard))
    }
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

  def removeSeed(id: String = clusterSeedId): ju.HashMap[String, AnyVal] =
    run(_.table(Tables.ClusterSeed.name).get(id).delete())

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
    val value: Any = getValueResult(
      tablename = Tables.Status.name,
      idValue = ActionStatus.name,
      defaultValue = ActionStatus.init.toString
    )
    val actionStatus = ActionStatus.withName(value.toString)
    actionStatus
  }

  def getValueResult[A](dbname: String = dbname, tablename: String, idValue: String, fieldname: String = value, defaultValue: A) =
    run[A](_ => getValue(dbname, tablename, idValue, fieldname, defaultValue))

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
    val row = r.hashMap(field.taskType.toString, task.actionState.toString)
      .`with`(field.workerId.toString, workerId)
      .`with`(field.clusterId.toString, clusterId)
      .`with`(field.createTime.toString, OffsetDateTime.now())
      .`with`(field.pending.toString, pending)
    val taskId: String = run(r => r.table(Tables.Task.name).insert(row).getField(generated_keys).nth(0))
    Log.debug(s"new task saved to database, taskid: $taskId")
    taskId
  }

  def reassignTask(taskId: String, clusterId: String, workerId: String) = {
    val field = Tables.Task.Field
    run[Any](r => r.table(Tables.Task.name).get(taskId).update(
      r.hashMap(field.workerId.toString, workerId)
        .`with`(field.clusterId.toString, clusterId)
        .`with`(field.createTime.toString, OffsetDateTime.now())
        .`with`(field.pending.toString, false)
    ))
  }

  def getActiveTasksByWorkerId(workerId: String): Seq[Task] = {
    val fs = Tables.Task.Field
    runToBuffer[ju.Map[String, AnyRef]](r => r.table(Tables.Task.name)
      .without(fs.completeTime.toString)
      .filter(r.hashMap(fs.workerId.toString, workerId)
      )
    ).map(toTask)
  }

  def finishTask(taskId: String): ju.HashMap[String, AnyRef] = {
    assert(taskId != null, s"Error : taskid is null!")
    run(r => r.table(Tables.Task.name).get(taskId).update(reqlFunction1(row =>
      r.hashMap(Tables.Task.Field.completeTime.toString, OffsetDateTime.now())
    )))
  }

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

  def runToBuffer[A](fun: RethinkDB => ReqlAst, retry: Boolean = true): mutable.Buffer[A] = conn.synchronized({
    assert(r != null)
    assert(fun != null)
    assert(conn != null)
    def main(): mutable.Buffer[A] = {
      val buffer = fun(r).run[Object](conn) match {
        case cursor: Cursor[A] =>
          //Log.debug(s"buliding buffer from cursor $cursor")
          cursorToBuffer[A](cursor)
        case xs: ju.List[A] =>
          //Log.debug(s"building buffer from list $xs")
          xs.asInstanceOf[ju.List[Object]].asScala.filter(x => {
            //Log.debug(s"check type of $x")
            x.isInstanceOf[A]
          }).map(_.asInstanceOf[A])
      }
      //Log.debug(s"built buffer of length ${buffer.length}")
      buffer
    }
    try {
      main()
    } catch {
      case e: ReqlDriverError =>
        fork(runnable(() => {
          throw e
        }))
        Log.error("try to reconnect database", e)
        conn.reconnect()
        main()
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

  def markTrainSample(percentage: Double): Long = {
    //DatabaseHelper_.markTrainSample_(Tables.RawData.name, Tables.RawData.Field.isTrain.toString, percentage)
    val t: String = Tables.RawData.name
    val f: String = Tables.RawData.Field.isTrain.toString
    /* 1. calculate count */
    Log.info("mark Train Sample (1/3) : calculate sample count")
    val totalCount: Long = DatabaseHelper.run(_.table(t).hasFields(f).count())
    val count = Math.round(totalCount * percentage)
    Log.debug(s"totalCount:$totalCount\tcount:$count")
    /* 2. set all to false */
    Log.info("mark Train Sample (2/3) : set all to false")
    DatabaseHelper.run[ju.Map[String, AnyRef]](_.table(t).hasFields(f).update(r.hashMap(f, false)))
    /* 3. set some to true */
    Log.info("mark Train Sample (3/3) : set some to true")
    DatabaseHelper.run[ju.Map[String, AnyVal]](_.table(t).hasFields(f).sample(count).update(r.hashMap(f, true)).optArg(return_changes, false))
    count
  }

  /** @param field         : hand | ankle | chest
    * @param trainTestFlag : isTrain | isTest */
  @throws(classOf[NoSuchElementException])
  @deprecated("too rough")
  def getIMU(field: String, count: Long, trainTestFlag: String): Stream[ju.Map[String, AnyRef]] = {
    val fs = Tables.RawData.Field
    DatabaseHelper.run[ju.List[ju.Map[String, AnyRef]]](r => r.table(Tables.RawData.name)
      .filter(r.hashMap(trainTestFlag, true))
      .getField(fs.timeSequence.toString)
      .concatMap(reqlFunction1(row => row.getField(field)))
      .sample(count)
    ).iterator().asScala.toStream
  }

  /** @param trainTestFlag : isTrain | isTest **/
  def getIMUPart(label: ImuSomTrainingTask.LabelType, count: Long, trainTestFlag: String) = {
    val fs = Tables.RawData.Field
    val x = label + "x"
    val y = label + "y"
    val z = label + "z"
    runToBuffer[ju.Map[String, Double]](r => r.table(Tables.RawData.name)
      .filter(trainTestFlag, true)
      .getField(fs.timeSequence.toString)
      .concatMap(reqlFunction1(row =>
        row.getField(fs.hand.toString).withFields(x, y, z)
          .add(row.getField(fs.ankle.toString).withFields(x, y, z))
          .add(row.getField(fs.chest.toString).withFields(x, y, z))
      ))
      .sample(count)
    )
  }


  def saveSom(som: Som) =
    tableInsertRow(Tables.SomImage.name, som.toMap)

  def loadSom(labelPrefix: String): Option[Som] = {
    try {
      val xs = DatabaseHelper.run[ju.List[ju.Map[String, AnyRef]]](r => r.table(Tables.SomImage.name)
        .filter(r.hashMap(Tables.SomImage.LabelPrefix, labelPrefix))
      )
      if (xs.isEmpty)
        None
      else
        Som.fromMap(xs.get(0))
    } catch {
      case e: Exception =>
        Log.error(e)
        fork(() => throw e)
        None
    }
  }

  def loadSubject(subjectId: String) =
    runToBuffer[ju.Map[String, AnyRef]](r => r.table(Tables.Subject.name)
      .filter(r.hashMap(Tables.Subject.Field.subject_id.toString, subjectId))
    ).get(0)

  implicit def cursorToBuffer[A](implicit cursor: Cursor[A], skipNull: Boolean = true) = {
    val buffer = mutable.Buffer.empty[A]
    try {
      cursor.iterator().asScala.foreach(x => if (x != null || skipNull) buffer += x)
    } catch {
      case e: NoSuchElementException =>
      case e: NullPointerException =>
        Log.error(s"Error : cursorToBuffer $buffer")
        throw e
    }
    buffer
  }

  def toTask(map: ju.Map[String, AnyRef]): Task = {
    import Tables.Task.{Field => field}
    import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.Task.Param
    import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.Task.Label
    val taskType = map.get(field.taskType.toString)
    if (taskType == null) {
      Log.error(s"unknown task type $map")
      fork(() => throw ???)
    }
    val fs = Tables.RawData.Field
    val fs2 = Tables.Subject.Field
    ActionStatus.withName(taskType.toString) match {
      case ActionStatus.somProcess => map.get(Param).asInstanceOf[ju.Map[String, AnyRef]].get(Label) match {
        case s: String if s.equals(Tables.IMU.Field.temperature.toString) => TemperatureSomTrainingTask.fromMap(map)
        case s: String if s.equals(fs.heartRate.toString) => HeartRateSomTrainingTask.fromMap(map)
        case s: String if s.equals(fs2.weight.toString) => WeightSomTrainingTask.fromMap(map)
        case s: String if s.equals(fs2.height.toString) => HeightSomTrainingTask.fromMap(map)
        case s: String if s.equals(fs2.age.toString) => AgeSomTrainingTask.fromMap(map)
        case s: String => ImuSomTrainingTask.fromMap(map)
        case label => Log.error(s"unknown task type $taskType, label: $label")
          fork(() => throw ???)
          null
      }
      case actionStatus: ActionStatusType => TaskResolvers.find(_.actionState.equals(actionStatus)) match {
        case Some(taskResolver) => taskResolver.fromMap(map)
        case None => Log.error(s"unknown task type $taskType")
          fork(() => throw ???)
          null
      }
    }
  }

  def getItemSetSize: Long = DatabaseHelper.getValueResult(
    tablename = Tables.Status.name,
    idValue = Tables.Status.Field.itemsetSize.toString,
    defaultValue = 1L
  )

  def setItemSetSize(size: Long) = DatabaseHelper.setValue(
    tablename = Tables.Status.name,
    idValue = Tables.Status.Field.itemsetSize.toString,
    newVal = size
  )

  def getArmLNum: Long = DatabaseHelper.getValueResult(
    tablename = Tables.Status.name,
    idValue = Tables.Status.Field.armLNum.toString,
    defaultValue = 1L
  )

  def setArmLNum(num: Long) = DatabaseHelper.setValue(
    tablename = Tables.Status.name,
    idValue = Tables.Status.Field.armLNum.toString,
    newVal = num
  )

  def countTableItem(table: Table, filter: MapObject): Long =
    countTableItem(table, Seq(filter))

  def countTableItem(table: Table, filters: Seq[MapObject]): Long =
    run[Long](r => {
      var query: ReqlExpr = r.table(table.name)
      filters.foreach(filter => query = query.filter(filter))
      query.count()
    })

  def getTableRowMapObject(table: Table, id: String): ju.Map[String, AnyRef] =
    run(_.table(table.name).get(id))

  def getTableRowMapObject(table: Table, offset: Long): ju.Map[String, AnyRef] =
    runToBuffer(_.table(table.name).skip(offset).limit(1)).get(0)
}
