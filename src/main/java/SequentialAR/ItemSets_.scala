package SequentialAR


/**
  * Created by beenotung on 4/13/16.
  * converted from ItemSets.java
  */
class ItemSets_(val dataset: IndexedSeq[IndexedSeq[String]]) {
  var count = 0

  override def toString: String = {
    var str: String = ""
    str += "{"
    var i: Int = 0
    while (i < dataset.length) {
      {
        str += "{"
        var j: Int = 0
        while (j < dataset(i).length) {
          {
            str += dataset(i)(j)
            if (j < dataset(i).length - 1) {
              str += ", "
            }
          }
          j += 1
        }
        str += "}"
        if (i < dataset.length - 1) {
          str += ","
        }
      }
      i += 1
    }
    str += "}"
    str
  }

  def isContain(sequence_set: IndexedSeq[IndexedSeq[String]]): Boolean = {
    var i: Int = 0
    var time_no: Int = 0
    var item_no: Int = 0
    while (time_no < sequence_set.length) {
      {
        item_no = 0
        while (item_no < sequence_set(time_no).length) {
          {
            if (i == dataset.length) {
              return false
            }
            if (!rowHaveItem(sequence_set(time_no)(item_no), i)) {
              if (i >= dataset.length - 1) {
                return false
              }
              else {
                i += 1
                item_no = -1
              }
            }
          }
          item_no += 1
        }
        i += 1
      }
      time_no += 1
    }
    true
  }

  def rowHaveItem(sequence_col: String, time_no: Int): Boolean = {
    var j: Int = 0
    while (j < dataset(time_no).size) {
      if (sequence_col.equals(dataset(time_no)(j)))
        return true
      j += 1
    }
    false
  }

}
