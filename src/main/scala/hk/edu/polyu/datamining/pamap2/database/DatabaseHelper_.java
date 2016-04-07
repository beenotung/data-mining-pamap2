package hk.edu.polyu.datamining.pamap2.database;

import com.rethinkdb.gen.ast.Filter;
import com.rethinkdb.gen.ast.Map;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;

import java.util.List;

import static hk.edu.polyu.datamining.pamap2.database.DatabaseHelper.*;

/**
 * Created by beenotung on 2/3/16.
 */
public class DatabaseHelper_ {
  private static Connection conn;

  static void setConn(Connection conn) {
    DatabaseHelper_.conn = conn;
  }

  public static Filter selectServers(String tag) {
    return r().db(rethinkdb()).table(server_config())
        .filter(table -> table.getField(tags()).contains(tag));
  }

  public static Map selectServerIds(String tag) {
    return selectServers(tag).map(server -> server.getField("id"));
  }

  public static List selectServerIdsResult(String tag) {
    Cursor result = selectServerIds(tag).run(conn);
    return result.toList();
  }

  public static void markTrainSample_(final String table, final String field, final double percentage) {
    final double threshold = (percentage <= 1) ? percentage : percentage / 100d;
    r().table(table).hasFields(field).forEach(x -> x.update(r().hashMap(field, r().random().optArg("float", true).lt(threshold)))).run(conn);
  }
}
