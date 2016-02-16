package hk.edu.polyu.datamining.pamap2.association_rule

import javafx.beans.property.{BooleanProperty, SimpleBooleanProperty, SimpleStringProperty, StringProperty}

import scala.collection.JavaConverters._

/**
  * Created by beenotung on 2/2/16.
  */
class Process(name: String, supportAuto: Boolean, defaultAuto: Boolean = false) {
  val nameProperty: StringProperty = new SimpleStringProperty(this, "name", name)
  val supportAutoProperty: BooleanProperty = new SimpleBooleanProperty(this, "supportAuto", supportAuto)
  val isAutoProperty: BooleanProperty = new SimpleBooleanProperty(this, "isAuto", false)
}

object Process {
  val ExtractFromRaw = new Process("Extract from raw", true)
  val ExtractSequence = new Process("Extract sequence", true)
  val Learn = new Process("Mine rules", true)
  val Test = new Process("Test rules", true)
  val all: java.util.Collection[Process] = Seq(ExtractFromRaw, ExtractSequence, Learn, Test).asJava
}
