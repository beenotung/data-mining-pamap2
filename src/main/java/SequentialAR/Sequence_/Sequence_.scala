package SequentialAR.Sequence_

/**
  * Created by beenotung on 4/13/16.
  * wrapper for Sequence.java
  */

import java.{util => ju}

import SequentialAR.{ItemSets, Sequence}
import hk.edu.polyu.datamining.pamap2.utils.Lang_

import scala.collection.JavaConverters._

object Sequence_ {
  type ItemSet = ju.List[String]
  type ItemSetSeq = ju.List[ItemSet]

  def createFirstSeq(activitySeq: IndexedSeq[ju.List[String]]): ju.List[ItemSetSeq] = {
    val dataset = Lang_.toArray(activitySeq.map(Lang_.toArray[String]).toList.asJava)
    val activity = new ItemSets(dataset)
    val one_seq_sets = Sequence.createFirstSeq(Array(activity))
    one_seq_sets.asScala.map(one_seq_set => {
      one_seq_set.getItemSets.map(_.toList.asJava).toList.asJava
    }).toList.asJava
  }
}
