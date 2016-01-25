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

    def fields: Seq[String]
  }

  object Status extends Table {
    val actionStatus = "actionStatus"

    override def name: String = "status"

    override def fields: Seq[String] = Array(actionStatus)
  }

  object RawData extends Table {
    override def name: String = "raw_data"

    val timestamp = "timestamp"

    override def fields: Seq[String] = Array(timestamp)
  }

  /* same as RawData, will has extra flag to indicate test result? */
  object TestingData extends Table {
    override def name: String = "testing_data"

    val timestamp = "timestamp"

    override def fields: Seq[String] = Array(timestamp)
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
