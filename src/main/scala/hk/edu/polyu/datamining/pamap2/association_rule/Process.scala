package hk.edu.polyu.datamining.pamap2.association_rule

import java.util
import javafx.beans.property.{BooleanProperty, SimpleBooleanProperty, SimpleStringProperty, StringProperty}

import SequentialAR.{ItemSets, Sequence}

import scala.collection.JavaConverters._

/**
  * Created by beenotung on 2/2/16.
  */
class Process(name: String, supportAuto: Boolean, defaultAuto: Boolean = false) {
  val nameProperty: StringProperty = new SimpleStringProperty(this, "name", name)
  val supportAutoProperty: BooleanProperty = new SimpleBooleanProperty(this, "supportAuto", supportAuto)
  val isAutoProperty: BooleanProperty = new SimpleBooleanProperty(this, "isAuto", false)

  def createSeqList(activity:Array[ItemSets]: Array[ItemSets] ={
    var seq_sets = Sequence.createFirstSeq(activity)
    seq_sets = Sequence.countActSeq(activity, seq_sets)
  }
}

object Process {
  lazy val ExtractItem = new Process("Extract item", true)
  lazy val CountItem = new Process("Count item", true)
  lazy val ExtractSequence = new Process("Extract sequence", true)
  lazy val MineRule = new Process("Mine rule", true)
  lazy val TestRule = new Process("Test rule", true)

  lazy val all: java.util.Collection[Process] = Seq(ExtractItem, CountItem, ExtractSequence, MineRule, TestRule).asJava

}
