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

}

object Process {
  lazy val ExtractItem = new Process("Extract item", true)
  lazy val CountItem = new Process("Count item", true)
  lazy val ExtractSequence = new Process("Extract sequence", true)
  lazy val MineRule = new Process("Mine rule", true)
  lazy val TestRule = new Process("Test rule", true)

  lazy val all: java.util.Collection[Process] = Seq(ExtractItem, CountItem, ExtractSequence, MineRule, TestRule).asJava


  /**
    * createFirstSeq
    *
    * @param activity
    * @return
    */
  def createFirstSeq (activity: Array[ItemSets]): util.ArrayList[ItemSets] = {
    val one_seq_sets = new util.ArrayList[ItemSets]()
    for (act <- 0 to activity.length){
      val activity_iset = activity(act).getItemSets()
      for(time_no<-0 to activity_iset.length){
        val time_items = activity_iset(time_no)

        for(i<-0 to time_items.length){
          val input = new Array[Array[String]](1)(time_items.length)
          input(0) = time_items
          val input_iset:ItemSets = new ItemSets(input)
          if (!one_seq_sets.toString.contains(input_iset.toString)) {
            one_seq_sets.add(input_iset)
          }
        }
      }
    }

    for (i<-0 to one_seq_sets.size()){
      val act_row = one_seq_sets.get(i).getItemSets()
      for(act2<-0 to activity.length){
        if (act_row(0).length - 1 == 0) {
          break //todo: break is not supported
        }else{
          val a = Sequence.cutingInRow(act_row(0))
          for (i2 <- 0 to a.length){
            val input = new Array[Array[String]](1)(act_row(0).length -1)
            input(0) = a(i2)
            val input_iset = new ItemSets(input)
            if (!one_seq_sets.toString().contains(input_iset.toString())) {
              one_seq_sets.add(input_iset);
            }
          }
        }
      }
    }
  }

}
