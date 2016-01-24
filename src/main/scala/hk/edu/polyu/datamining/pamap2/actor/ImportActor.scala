package hk.edu.polyu.datamining.pamap2.actor

import akka.actor.Actor
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.ImportCommand

/**
  * Created by beenotung on 1/21/16.
  */
object ImportActor {

  final case class ImportCommand(filename: String, lineNumbers: Seq[Int])

}

class ImportActor extends Actor {
  override def receive: Receive = {
    case ImportCommand(filename, lineNumbers) =>
  }
}
