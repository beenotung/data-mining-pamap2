package hk.edu.polyu.datamining.pamap2.database;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import hk.edu.polyu.datamining.pamap2.utils.Lang;

import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by beenotung on 1/25/16.
 */
public class DatabaseHelper {
  public static final Connection conn;
  public static final RethinkDB r = RethinkDB.r;
  public static final String hostname;
  public static final int port;
  public static final String dbname;

  static {
    Connection conn1;
    Config config = ConfigFactory.parseResources("database.conf");
    hostname = config.getString("rethinkdb.host");
    port = config.getInt("rethinkdb.port");
    dbname = config.getString("rethinkdb.dbname");
    try {
      conn1 = r.connection()
          .hostname(hostname)
          .port(port)
          .db(dbname)
          .connect();
    } catch (TimeoutException e) {
      conn1 = null;
      e.printStackTrace();
      System.err.println("Failed to connect to database, exiting...");
      System.exit(1);
    }
    conn = conn1;
  }

  public static HashMap<String, Object> createDatabaseIfNotExist(String dbname) {
    return r.dbList().contains(dbname).do_(exist -> r.branch(
        exist,
        new HashMap<String, Object>() {{
          put("created", 0);
        }},
        r.dbCreate(dbname)
        )
    ).run(conn);
  }

  public static HashMap<String, Object> createTableDropIfExist(String tableName) {
    r.db(dbname).table(tableName).delete().run(conn);
    return r.db(dbname).tableCreate(tableName).run(conn);
  }

  public static boolean hasInit() {
    return (boolean) r.dbList().contains(dbname).do_(dbExist -> {
      String tableName = Tables.Status$.MODULE$.name();
      return r.branch(
          dbExist,
          r.db(dbname).tableList().contains(tableName).do_(tableExist -> r.branch(
              tableExist,
              r.db(dbname).table(tableName).withFields(Tables.Status$.MODULE$.actionStatus()).count().ge(1),
              false
              )
          ),
          false
      );
    }).run(conn);
  }

  public static void init(String currentStatus, String nextStatus) {
    String statusTableName = Tables.Status$.MODULE$.name();
    String statusFieldName = Tables.Status$.MODULE$.actionStatus();
    /* drop and create database */
    r.dbList().contains(dbname).do_(dbExist -> r.branch(
        dbExist,
        r.dbDrop(dbname),
        r.hashMap("dbs_droppped", 0)
    )).run(conn);
    r.dbCreate(dbname).run(conn);
    conn.use(dbname);
    /* create status table */
    r.tableCreate(statusTableName).run(conn);
    r.table(statusTableName).insert(r.hashMap(statusFieldName, currentStatus));
    /* create other tables */
    Lang.seqToList(Tables.tableNames()).stream()
        .filter(t -> !statusTableName.equals(t))
        .forEach(t -> r.tableCreate(t).run(conn));
    /* update status table */
    r.table(statusTableName).update(r.hashMap(statusFieldName, nextStatus));
  }
}
