package hk.edu.polyu.datamining.pamap2.ui

import java.text.SimpleDateFormat
import java.util.Locale
import javafx.application.Platform.{runLater => runOnUIThread}
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, Button, Label}
import javafx.scene.layout.VBox
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import javafx.util.Duration

import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.UnRegisterComputeNode
import hk.edu.polyu.datamining.pamap2.actor.UIActor
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.ui.NodesDetailController._
import hk.edu.polyu.datamining.pamap2.utils.FormatUtils._
import hk.edu.polyu.datamining.pamap2.utils.Lang
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable

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

  def updateList() = runOnUIThread(runnable(() => if (instance != null) instance.updateList()))
}

class NodesDetailController extends NodesDetailControllerSkeleton {
  NodesDetailController.instance = this

  override def customInit() = {
    updateList()
  }

  def updateList() = {
    val nodes = MonitorController.clusterComputeInfo.map { node => {
      val nodeInfo = node.nodeInfo
      val name = {
        val (host, port, roles) = DatabaseHelper.getHostInfo(nodeInfo.clusterSeedId)
        s"$host:$port $roles (${nodeInfo.clusterSeedId})"
      }
      val vbox = new VBox(NodesDetailController.spacing)
      vbox.getChildren.addAll(
        new Label("_" * nodeInfo.clusterSeedId.length),
        new Label(name), {
          val btn = new Button("remove")
          btn.setOnAction(Lang.eventHandler(event => Lang.fork(() => {
            DatabaseHelper.removeSeed(nodeInfo.clusterSeedId)
            UIActor.dispatch(UnRegisterComputeNode(nodeInfo.clusterSeedId))
            runOnUIThread(Lang.runnable(() => {
              val alert = new Alert(AlertType.INFORMATION)
              alert.setTitle("Success")
              alert.setHeaderText(s"The node $name has been removed")
              alert.show()
            }))
          })))
          btn
        },
        new Label({
          val processorUsage = node.workerRecords.length + " / " + nodeInfo.processor
          val used: Long = nodeInfo.totalMemory - nodeInfo.freeMemory
          val usage = 100 * used / nodeInfo.maxMemory
          val memUsage = s"${formatSize(used)} / ${formatSize(nodeInfo.maxMemory)} ($usage%)"
          val pending = node.workerRecords.map(_.pendingTask).sum
          val completed = node.workerRecords.map(_.pendingTask).sum
          val starttime = formatDate(nodeInfo.startTime)
          val reporttime = formatDate(nodeInfo.genTime)
          val uptime = formatDuration(nodeInfo.upTime)
          s"processor : $processorUsage\n" +
            s"memory usage : $memUsage\n" +
            s"pending task : $pending\n" +
            s"completed task : $completed\n" +
            s"start time : $starttime\n" +
            s"last report time : $reporttime\n" +
            s"uptime : $uptime"
        }))
      vbox
    }
    }
    messageLabel setText s"${MonitorController.clusterComputeInfo.size} host(s)"
    detailsLabel setText " "
    main_vbox.getChildren.clear()
    main_vbox.getChildren.addAll(nodes.asJavaCollection)
  }

  override def close_window(event: ActionEvent) = {
    stage.close()
    stage = null
    instance = null
  }
}
