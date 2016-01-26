package hk.edu.polyu.datamining.pamap2.database


/**
  * Created by beenotung on 1/26/16.
  */
//TODO to complete these tables
object Tables {

  val tableList = Seq(Status, RawData, TestingData, ExtractedData, ItemsetCount, AssociationRule)
  val tableNames = tableList map (_.name)

  sealed trait Table {
    def name: String

    def fields: Iterable[String]
  }

  object Status extends Table {
    override def name: String = "status"

    object Field extends Enumeration {
      type Field = Value
      val actionStatus, pendingIds, dispatchedIds = Value
    }

    override def fields: Iterable[String] = Field.values.map(_.toString)
  }

  object IMU extends Table {
    override def name: String = "IMU"

    object Field extends Enumeration {
      type Field = Value
      val temperature, a16x, a16y, a16z, a6x, a6y, a6z, rx, ry, rz, mx, my, mz = Value
    }

    override def fields: Iterable[String] = Field.values.map(_.toString)
  }

  object RawData extends Table {
    override def name: String = "raw_data"

    object Field extends Enumeration {
      type Field = Value
      val timestamp, activityId, heartRate, hand, chest, ankle, skip = Value
    }

    override def fields = Field.values.map(_.toString)
  }

  /* same as RawData, will has extra flag to indicate test result? */
  object TestingData extends Table {
    override def name: String = "testing_data"

    object Field extends Enumeration {
      type Field = Value
      val timestamp, activityId, heartRate, hand, chest, ankle = Value
    }

    override def fields = Field.values.map(_.toString)
  }

  object ExtractedData extends Table {
    override def name: String = "extracted_data"

    val timestamp = "timestamp"

    override def fields: Seq[String] = Array(timestamp)
  }

  object ItemsetCount extends Table {
    override def name: String = "itemset_count"

    val itemset = "itemset"

    override def fields: Seq[String] = Array(itemset)
  }

  object AssociationRule extends Table {
    override def name: String = "association_rule"

    val predicateSet = "predicateSet"

    override def fields: Seq[String] = Array(predicateSet)
  }

}
