package SequentialAR

/**
  * Created by beenotung on 4/13/16.
  * converted from Sequence.java
  */

import scala.collection.JavaConverters._
import scala.util.control.Breaks._
import java.util

import hk.edu.polyu.datamining.pamap2.utils.Log

import scala.collection.mutable

object Sequence_ {
  def createFirstSeq(activity: Seq[ItemSets_]): util.List[ItemSets_] = {
    val one_seq_sets = mutable.Buffer.empty[ItemSets_]
    activity.foreach(act => {
      val activity_itemset = act.dataset
      activity_itemset.foreach(time_items => {
        val input = IndexedSeq(time_items)
        val input_itemset = new ItemSets_(input)
        if (!one_seq_sets.toString().contains(input_itemset.toString)) {
          Log debug "add input itemset"
          one_seq_sets += input_itemset
        } else {
          Log debug "skip input itemset"
        }
      })
    })
    one_seq_sets.foreach(i => {
      val act_row = i.dataset
      breakable {
        activity.foreach(act2 => {
          if (act_row(0).length - 1 == 0)
            break()
          else {
            val a = cutingInRow(act_row(0))
            a.foreach(i2 => {
              val input = IndexedSeq(i2.toIndexedSeq)
              val input_itemset = new ItemSets_(input)
              if (!one_seq_sets.toString().contains(input_itemset.toString))
                one_seq_sets += input_itemset
            })
          }
        })
      }
    })
    one_seq_sets.toList.asJava
  }

  def cutingInRow(item_input: IndexedSeq[String]): IndexedSeq[IndexedSeq[String]] = {
    var n: Int = 0
    item_input.indices.map(n => {
      item_input.indices.map(i => {
        if (i != n) {
          Some(item_input(i))
        } else {
          None
        }
      })
        .filter(_.isDefined).map(_.get)
    })
  }

  //  def reduceDuplicate(all_seq_sets: util.List[util.List[ItemSets_]]): util.List[util.List[ItemSets_]] = {
  //    (all_seq_sets.size() - 2).to(end = 0, step = -1).foreach(i => {
  //      val now_itemsets = mutable.Buffer.empty[ItemSets_]
  //      all_seq_sets.get(i).size()
  //    })
  //  }
}

class Sequence_ {

}
