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
  val mapRawDataToItem = Value
  val itemSetGeneration, itemSetReduction = Value
  val sequenceGeneration, sequenceReduction = Value
  val ruleGeneration, ruleReduction = Value
  val testing = Value
  val finished = Value

  def next(actionStatusType: ActionStatusType@NotNull): ActionStatusType = actionStatusType match {
    case `init` => importing
    case `importing` => imported
    case `imported` => sampling
    case `sampling` => somProcess
    case `somProcess` => mapRawDataToItem
    case `mapRawDataToItem` => itemSetGeneration
    case `itemSetGeneration` => itemSetReduction
    case `itemSetReduction` => sequenceGeneration
    case `sequenceGeneration` => sequenceReduction
    case `sequenceReduction` => ruleGeneration
    case `ruleGeneration` => ruleReduction
    case `ruleReduction` => testing
    case `testing` => finished
    case `finished` => finished
  }
}

