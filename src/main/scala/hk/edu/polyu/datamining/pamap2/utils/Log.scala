package hk.edu.polyu.datamining.pamap2.utils

import java.io._

import com.typesafe.config.ConfigFactory

/**
  * Created by beenotung on 3/24/16.
  */

import hk.edu.polyu.datamining.pamap2.utils.Lang._

object Log {
  lazy val (isError, isDebug, isLog, logFilename) = {
    val conf = ConfigFactory parseResources "application.conf"
    val isError = conf getBoolean "log.isError"
    val isDebug = conf getBoolean "log.isDebug"
    val isLog = conf getBoolean "log.isLog"
    val logFilename = conf getString "log.filename"
    (isError, isDebug, isLog, logFilename)
  }
  private lazy val logStream = {
    val stream = new PrintWriter(new File(logFilename))
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      stream.close()
    }))
    stream
  }

  def debug(x: Any*) = if (isDebug)
    if (x.length == 1)
      println(x.head)
    else
      println(x)

  def error(x: Any*): Unit = if (isError)
    if (x.length == 1)
      System.err.println(x.head)
    else
      System.err.println(x)

  def log(x: Any*) = if (isLog)
    if (x.length == 1)
      logStream.println(x.head)
    else
      logStream.println(x)
}
