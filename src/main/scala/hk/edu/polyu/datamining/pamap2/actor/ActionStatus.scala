package hk.edu.polyu.datamining.pamap2.actor

/**
  * Created by beenotung on 1/29/16.
  */
object ActionStatus extends Enumeration {
  type Status = Value
  val checkStatus, init, importing, preProcess, learning, testing = Value
}