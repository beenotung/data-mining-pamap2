package hk.edu.polyu.datamining.pamap2.ui

import java.text.SimpleDateFormat
import java.util.Locale
import javafx.application.Platform.{runLater => runOnUIThread}
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import javafx.util.Duration

import hk.edu.polyu.datamining.pamap2.ui.NodesDetailController._
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.JavaConverters._

/**
  * Created by beenotung on 2/10/16.
  */
object NodesDetailController {
  val spacing = 7
  val dateFormat = {
    val format = "yyyy-MM-dd HH:mm:ss.SSSZ"
    val locale: Locale = Locale.TRADITIONAL_CHINESE
    new SimpleDateFormat(format, locale)
  }
  private[ui] var stage: Stage = null
  private[ui] var instance: NodesDetailController = null

  def start() = runOnUIThread(() => {
    if (stage != null)
      stage.close()
    stage = new Stage()
    stage.setTitle("Nodes Detail")
    val root: Parent = FXMLLoader.load(getClass.getResource("NodesDetail.fxml"))
    stage.setScene(new Scene(root))
    stage.show()
  })

  def formatDate(time: Long): String = {
    dateFormat.format(time)
  }

  def formatDuration(time: Long): String = {
    new Duration(time).toString
  }
}

class NodesDetailController extends NodesDetailControllerSkeleton {
  NodesDetailController.instance = this

  override def customInit() = {
    updateList()
  }

  def updateList() = {
    val nodes = MonitorController.nodes.asScala.map { node => {
      val vbox = new VBox(NodesDetailController.spacing)
      vbox.getChildren.addAll(
        new Label({
          val host = node.nodeAddress.hosts.toString
          val port = node.nodeAddress.port
          s"$host : $port"
        }),
        new Label({
          val starttime = formatDate(node.startTime)
          val uptime = formatDuration(node.upTime)
          val memUsage = 1d * (node.totalMemory - node.freeMemory) / node.maxMemory
          s"${node.processor} processor\n" +
            s"memory usage : $memUsage\n" +
            s"start time : $starttime\n" +
            s"uptime : $uptime"
        }))
      vbox
    }
    }
    messageLabel setText s"${nodes.size}/${MonitorController.nodes_num} host(s)"
    detailsLabel setText " "
    main_vbox.getChildren.addAll(nodes.asJavaCollection)
  }

  override def close_window(event: ActionEvent) = {
    stage.close()
    stage = null
  }
}
