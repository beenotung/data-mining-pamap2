package hk.edu.polyu.datamining.pamap2.database


/**
  * Created by beenotung on 1/26/16.
  */
//TODO to complete these tables
object Tables {

  val tableList = Seq(Status, RawData, TestingData, TestingResult, ItemsetCount, AssociationRule)
  val tableNames = tableList map (_.name)

  sealed trait Table {
    def name: String

    def fields: Iterable[String]
  }

  object Status extends Table {
    override def name: String = "status"

    override def fields: Iterable[String] = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val actionStatus, pendingIds, dispatchedIds = Value
    }

  }

  object IMU extends Table {
    val ExtractedField = Seq(Field.relativeTemperature, Field.polarRadius, Field.polarTheta, Field.polarPhi)

    override def name: String = "IMU"

    override def fields: Iterable[String] = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val temperature,
      a16x, a16y, a16z,
      a6x, a6y, a6z,
      rx, ry, rz,
      mx, my, mz,
      /* extracted fields */
      relativeTemperature,
      polarRadius, polarTheta, polarPhi = Value
    }

  }

  object RawData extends Table {
    override def name: String = "raw_data"

    override def fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val subject, timestamp, activityId, heartRate, hand, chest, ankle, skip = Value
    }

  }

  object TestingData extends Table {
    override def name: String = "testing_data"

    override def fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val timestamp, activityId, heartRate, hand, chest, ankle = Value
    }

  }

  object TestingResult extends Table {
    override def name: String = "testing_result"

    override def fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val minimum_support, sampling_size = Value
      /* Array[ testingDataId : correct<Boolean> ] */
      val results = Value
    }

  }

  object ItemsetCount extends Table {
    override def name: String = "itemset_count"

    override def fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      /* array[String] */
      val itemset = Value
      val count = Value
    }

  }

  object AssociationRule extends Table {
    override def name: String = "association_rule"

    override def fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val itemset, support, confidence, interest, useful = Value
    }

  }

}
