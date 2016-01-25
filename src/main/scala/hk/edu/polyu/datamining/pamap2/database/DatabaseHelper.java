package hk.edu.polyu.datamining.pamap2.database;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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

    public static HashMap<String, Object> createDatabaseIfNotExist() {
        return createDatabaseIfNotExist(dbname);
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

    public static final String TABLE_STATUS = "status";

    public static boolean hasInit() {
        boolean result = r.dbList().contains(dbname).do_(dbExist -> r.branch(
                dbExist,
                r.db(dbname).tableList().contains(TABLE_STATUS).do_(tableExist -> r.branch(
                        tableExist,
                        true,
                        false
                        )
                ),
                false
        )).run(conn);
        return result;
    }
}
