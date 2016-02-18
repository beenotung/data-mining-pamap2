/**
  * Created by janus on 13/2/2016.
  */
package hk.edu.polyu.datamining.pamap2.math

import scala.collection.mutable.HashMap

object Counting {

  /**
    * Counting Function
    * return counting result in mutable HashMap format
    *
    * @param itemset : target items for counting in 1d array e.g. ["a", "b"]
    * @param dataset : raw records in 2d array e.g. [["a", "c", "d"],["b"]]
    */

  def counting(itemset: Array[String], dataset: Array[Array[String]]): HashMap[String, Int] = {
    if (dataset.length <= 2) {
      val container = new HashMap[String, Int]()
      for (record <- dataset) {
        for (item <- record) {
          if (itemset contains (item)) {
            val v = container.getOrElseUpdate(item, 0)
            container.update(item, v + 1)
          }
        }
      }
      container
    } else {
      val (left, right) = dataset.splitAt(dataset.length / 2)
      val container = counting(itemset, left)
      val list2 = counting(itemset, right)
      list2.foreach { case (key, value) => {
        val v = container.getOrElseUpdate(key, 0)
        container.update(key, v + value)
      }
      }
      container
    }
  }

}
