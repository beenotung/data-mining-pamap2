package hk.edu.polyu.datamining.pamap2.actor

import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.Status

/**
  * Created by beenotung on 1/29/16.
  */
object ActionStatus extends Enumeration {
  type Status = Value
  val checkStatus, init, importing, preProcess, learning, testing = Value
}

trait ActionProcessingConfirm {
  val actionStatus: ActionStatus.Status
  val args: Seq[AnyVal]
}


trait ActionFinishConfirm {
  val actionStatus: ActionStatus.Status
  val args: Seq[AnyVal]
}

case class ProcessingImport(val filename: String) extends ActionProcessingConfirm {
  override val actionStatus: Status = ActionStatus.importing
  override val args: Seq[AnyVal] = filename
}

case class FinishedImport(val filename: String) extends ActionFinishConfirm {
  override val actionStatus: Status = ActionStatus.importing
  override val args: Seq[AnyVal] = filename
}
