package hk.edu.polyu.datamining.pamap2.database


/**
  * Created by beenotung on 1/26/16.
  */
//TODO to complete these tables
object Tables {

  sealed trait Table {
    def name: String

    def fields: Seq[String]
  }

  object Status extends Table {
    override def name: String = "status"

    val ActionStatus = "actionStatus"

    override def fields: Seq[String] = Array(ActionStatus)
  }

  object RawData extends Table {
    override def name: String = "raw_data"

    override def fields: Seq[String] = Array()
  }

  object TestingData extends Table {
    override def name: String = "testing_data"

    override def fields: Seq[String] = Array()
  }

  object ExtractedData extends Table {
    override def name: String = "extracted_data"

    override def fields: Seq[String] = Array()
  }

  object ItemsetCount extends Table {
    override def name: String = "itemset_count"

    override def fields: Seq[String] = Array()
  }

  object AssociationRule extends Table {
    override def name: String = "association_rule"

    override def fields: Seq[String] = Array()
  }

  val tableList = Seq(Status, RawData, TestingData, ExtractedData, ItemsetCount, AssociationRule)
}
