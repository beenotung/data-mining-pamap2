package hk.edu.polyu.datamining.pamap2.database

/**
  * Created by beenotung on 1/26/16.
  */

import com.rethinkdb.RethinkDB.r
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper.{conn, dbname}
import hk.edu.polyu.datamining.pamap2.utils.Lang._

object DatabaseHelper_ {
  def hasInit: Boolean = {
    val tableName: String = Tables.Status.name
    val fieldName: String = Tables.Status.Field.actionStatus.toString
    r.dbList().contains(dbname).do_(reqlFunction1(dbExist =>
      r.branch(
        dbExist,
        r.db(dbname).tableList().contains(tableName).do_(reqlFunction1(tableExist => r.branch(
          tableExist,
          r.db(dbname).table(tableName).withFields(fieldName).count().ge(1),
          false
        ))),
        false
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
