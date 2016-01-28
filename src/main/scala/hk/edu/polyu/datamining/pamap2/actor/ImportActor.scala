package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.{Actor, ActorLogging}
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.ImportFile

import scala.io.Source
import scala.language.postfixOps

/**
  * Created by beenotung on 1/21/16.
  */
object ImportActor {

  final case class ImportFile(filename: String)

  final case class HandleLines(filename: String, lineOffset: Int, lineCount: Int, lines: Iterable[String])

  def processIMU(cols: Array[String], offset: Int) = ???

  def processLine(line: String) = {
    val cols = line.split(" ")
    val timestamp = cols(0) toFloat
    val activityId = cols(1) toByte
    val heartRate = cols(2) toFloat
    val hand = processIMU(cols, 3)
  }
}

class ImportActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case ImportFile(filename) =>
      Source fromFile filename getLines() map ImportActor.processLine
      Source.fromFile(filename).getLines().map(ImportActor.processLine)
  }
}
