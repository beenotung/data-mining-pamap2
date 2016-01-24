import com.rethinkdb.RethinkDB.r

/**
  * Created by beenotung on 1/24/16.
  */
object Database extends App {
  val host = "58.96.176.223"
  val port = 28015
  val address = host + ":" + port
  val dbname = "pamap2"

  println("hello")

//  println(s"host : $host")
//  println(s"port : $port")
//  println(s"dbname : $dbname")

  val conn = r.connection()
    .hostname(host)
    .port(port)
    .db(dbname)
    .connect()

  val result = r.dbList().run(conn)


  println("bye")
}
