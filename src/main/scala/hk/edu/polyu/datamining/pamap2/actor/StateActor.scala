package hk.edu.polyu.datamining.pamap2.actor

import com.sun.istack.internal.NotNull
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.ActionStatusType

/**
  * Created by beenotung on 1/21/16.
  * representing the status of dispatch actor
  */
object ActionStatus extends Enumeration {
  type ActionStatusType = Value
  val name = "ActionStatus"
  val reset, init, importing, imported, sampling, somProcess = Value
  val itemExtract, itemReduce=Value
  val itemSetGeneration, itemSetReduction = Value
  val sequenceGeneration, sequenceReduction = Value
  val learning, testing, finished = Value

  def next(actionStatusType: ActionStatusType@NotNull): ActionStatusType = actionStatusType match {
    case `init` => importing
    case `importing` => imported
    case `imported` => sampling
    case `sampling` => somProcess
    case `somProcess` => itemExtract
    case `itemExtract` => itemReduce
    case `itemReduce` => itemSetGeneration
    case `itemSetGeneration` => itemSetReduction
    case `itemSetReduction` => sequenceGeneration
    case `sequenceGeneration` => sequenceReduction
    case `sequenceReduction` => learning
    case `learning` => testing
    case `testing` => finished
    case `finished` => finished
  }
}

object StateHelper {

  sealed trait Message

  case class ResponseStatus(actionState: ActionStatusType) extends Message

  case class SetStatus(actionState: ActionStatusType) extends Message

  case object AskStatus extends Message

  /** @deprecated */
  case object NextStatus extends Message

}
