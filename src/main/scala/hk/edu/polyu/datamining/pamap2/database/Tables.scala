package hk.edu.polyu.datamining.pamap2.database


/**
  * Created by beenotung on 1/26/16.
  */
//TODO to complete these tables
object Tables {

  val tableList = Seq(Debug, ClusterSeed, Status, Task, Subject, RawDataFile, RawData, TestingResult, SomImage, ItemsetCount, AssociationRule)
  val tableNames = tableList map (_.name)

  sealed trait Table {
    val name: String
    val fields: Iterable[String]
  }

  object Debug extends Table {
    override val name = "debug"
    override val fields = Seq.empty
  }

  object ClusterSeed extends Table {
    override val name = "cluster_seed"
    override val fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val host, port, roles, config = Value
    }

  }

  object Status extends Table {
    override val name: String = "status"
    override val fields: Iterable[String] = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val actionStatus = Value
      val armLNum = Value
      //val pendingIds, dispatchedIds = Value
    }

  }

  object Task extends Table {
    override val name: String = "task"
    override val fields: Iterable[String] = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val taskType, clusterId, workerId, createTime, completeTime, pending = Value
    }

  }

  @deprecated("slow in practise")
  object RawDataFile extends Table {
    override val name: String = "raw_datafile"
    override val fields: Iterable[String] = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val filename, lines, isTraining, isTesting = Value
    }

  }

  object IMU extends Table {
    override val name: String = "IMU"
    override val fields: Iterable[String] = Field.values.map(_.toString)
    val ExtractedField = Seq(Field.relativeTemperature, Field.polarRadius, Field.polarTheta, Field.polarPhi)

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

  /* mix training and testing data */
  object RawData extends Table {
    override val name: String = "raw_data"
    override val fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val subject, timestamp, activityId, heartRate, hand, chest, ankle = Value
      val isTrain, isTest = Value
      val timeSequence = Value
      val done = Value
    }

  }

  object Subject extends Table {
    override val name = "subject"
    override val fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val subject_id, sex, age, height, weight, resting_HR, max_HR, dominant_hand = Value
    }

  }

  object TestingResult extends Table {
    override val name: String = "testing_result"
    override val fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val minimum_support, sampling_size = Value
      /* Array[ testingDataId : correct<Boolean> ] */
      val results = Value
    }

  }

  object SomImage extends Table {
    override val name: String = "som_image"
    override val fields = Seq(
      Weights, GridWidth, GridHeight, LabelPrefix, Grids, D1, D2, Vector_s
    )
    lazy val Weights = "weights"
    lazy val GridWidth = "gridWidth"
    lazy val GridHeight = "gridHeight"
    lazy val LabelPrefix = "labelPrefix"
    lazy val Grids = "grids"
    lazy val D1 = "d1"
    lazy val D2 = "d2"
    lazy val Vector_s = "vector"
  }

  object ItemsetCount extends Table {
    override val name: String = "itemset_count"
    override val fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      /* array[label:String] */
      val itemset = Value
      val count = Value
    }

  }

  object SequenceItemSetCount extends Table {
    override val name: String = "sequence_itemset_count"
    override val fields: Iterable[String] = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      /* array[itemsetId:String] */
      val sequence = Value
      val count = Value
    }

  }

  object AssociationRule extends Table {
    override val name: String = "association_rule"
    override val fields = Field.values.map(_.toString)

    object Field extends Enumeration {
      type Field = Value
      val itemset, support, confidence, interest, useful = Value
    }

  }

}
