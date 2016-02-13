/**
  * Created by janus on 13/2/2016.
  */
package hk.edu.polyu.datamining.pamap2.math
import collection.mutable.HashMap

object Counting {

  /**
    * Map Function
    * return counting result in mutable HashMap format
    *
    * @param itemset: target items for counting
    * @param dataset: raw records in 2d array
    */

  def mapreduce (itemset: Array[String], dataset: Array[Array[String]]): HashMap[String,Int] = {
    if (dataset.length <= 2){
      val container = new HashMap[String,Int]()
      for(record <- dataset){
        for(item <- record){
          if (itemset contains(item)){
            val v = container.getOrElseUpdate(item, 0)
            container.update(item, v + 1)
          }
        }
      }
      container
    }else{
      val (left, right) = dataset.splitAt(dataset.length / 2)
      val container = mapreduce(itemset, left)
      val list2 = mapreduce(itemset, right)
      list2.foreach { case (key, value) => {
        val v = container.getOrElseUpdate(key, 0)
        container.update(key, v + value)
      } }
      container
    }
  }

}
