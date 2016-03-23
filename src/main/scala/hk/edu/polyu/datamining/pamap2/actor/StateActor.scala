package hk.edu.polyu.datamining.pamap2.actor

import com.sun.istack.internal.NotNull
import hk.edu.polyu.datamining.pamap2.actor.ActionState.ActionStatusType

/**
  * Created by beenotung on 1/21/16.
  * representing the status of dispatch actor
  */
object ActionState extends Enumeration {
  type ActionStatusType = Value
  val name = "ActionState"
  val reset, init, importing, imported, preProcess, learning, testing, finished = Value

  @deprecated
  def next(actionStatusType: ActionStatusType@NotNull): ActionStatusType = actionStatusType match {
    case `init` => importing
    case `importing` => imported
    case `imported` => preProcess
    case `preProcess` => learning
    case `learning` => testing
    case `testing` => finished
    case `finished` => finished
  }
}

object StateActor {

  sealed trait Message

  case class ResponseStatus(actionState: ActionStatusType) extends Message

  case class SetStatus(actionState: ActionStatusType) extends Message

  case object AskStatus extends Message

  /** @deprecated */
  case object NextStatus extends Message

}
