package hk.edu.polyu.datamining.pamap2.ui

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.ActionEvent
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.{Alert, TableView}
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import hk.edu.polyu.datamining.pamap2.actor.ActionState.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.ImportFile
import hk.edu.polyu.datamining.pamap2.actor.{StateActor, UIActor}
import hk.edu.polyu.datamining.pamap2.ui.association_rule.ProcessCell
import hk.edu.polyu.datamining.pamap2.utils.Lang
import hk.edu.polyu.datamining.pamap2.utils.Lang.runnable

import scala.io.Source

/**
  * Created by beenotung on 1/30/16.
  */
object MonitorController {
  private var instance: MonitorController = null

  def onActorSystemTerminated() = instance match {
    case controller: MonitorController if controller != null =>
      Platform runLater runnable(() => {
        val alert = new Alert(AlertType.ERROR)
        alert.setTitle("Error")
        alert.setHeaderText("Actor System is shutdown")
        alert.setContentText("check the console for more detail\ne.g. not implemented error (???)")
        alert.showAndWait()
        Platform.exit()
      })
    case _ =>
  }

  def importingFile(filename: String) = Platform runLater (() => {
    instance.left_status.setText(s"importing $filename")
  })


  def importedFile(filename: String) = Platform runLater (() => {
    instance.left_status.setText(s"imported $filename")
    instance.handleNextFile()
  })

  def restarted(reason: String) = Platform runLater (() => {
    instance.promptRestarted(reason)
  })

  def updateStatus(status: ActionStatusType): Unit = Platform runLater (() => {
    instance.updated_right_status(status)
  })
}

class MonitorController extends MonitorControllerSkeleton {
  MonitorController.instance = this
  var pendingFiles = new ConcurrentLinkedQueue[File]
  var handlingFile = false

  override def customInit() = {
    val stage = MonitorApplication.getStage()

    /*   init table   */
    /* define table coluns */
    association_rule_mining_process_table_process.setCellValueFactory(new PropertyValueFactory("name"))
    association_rule_mining_process_table_auto.setSortable(false)
    /* make column only show non-empty rows */
    association_rule_mining_process_table_auto.setCellValueFactory(Lang.callback(
      (feature: javafx.scene.control.TableColumn.CellDataFeatures[hk.edu.polyu.datamining.pamap2.association_rule.Process, hk.edu.polyu.datamining.pamap2.association_rule.Process]) => {
        feature.getValue
      }))
    /* create button for each row */
    association_rule_mining_process_table_auto.setCellFactory(Lang.callback(
      (column) => {
        new ProcessCell(stage, association_rule_mining_process_table)
      }
    ))
    /* init rows */
    association_rule_mining_process_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY)
    association_rule_mining_process_table.getItems.addAll(hk.edu.polyu.datamining.pamap2.association_rule.Process.all)
    /*   get cluster status   */
    update_right_status(new ActionEvent())
  }

  override def update_right_status(event: ActionEvent) = {
    right_status.setText("getting cluster status")
    UIActor ! StateActor.AskStatus
  }

  def promptRestarted(reason: String): Unit = {
    val alert = new Alert(AlertType.WARNING)
    alert.setTitle("Warning")
    alert.setHeaderText("Service Restarted")
    alert.setContentText(reason)
    alert.showAndWait()
    left_status.setText("")
  }

  override def select_import_file(event: ActionEvent) = {
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Import File")
    fileChooser.getExtensionFilters.addAll(
      new ExtensionFilter("Data Files", "*.dat")
    )
    fileChooser.showOpenMultipleDialog(MonitorApplication.getStage) match {
      case files: List[File] if files != null => pendingFiles.addAll(files)
        if (!handlingFile) handleNextFile()
      case _ =>
    }
  }

  def handleNextFile() = {
    val file = pendingFiles.poll()
    if (file != null) {
      UIActor ! new ImportFile(file.getName, Source.fromFile(file).getLines().toSeq)
    }
  }

  def updated_right_status(statusType: ActionStatusType) = {
    right_status.setText(statusType.toString)
  }
}
