package hk.edu.polyu.datamining.pamap2.ui

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter
import scala.collection.JavaConverters._

import hk.edu.polyu.datamining.pamap2.actor.ActionState.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.{FileType, ImportFile}
import hk.edu.polyu.datamining.pamap2.actor.{StateActor, UIActor}
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
  var pendingFileItems = new ConcurrentLinkedQueue[(File,FileType)]
  var handlingFile = false

  override def customInit() = {
    /* get cluster status */
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

  override def select_training_datafile(event: ActionEvent) = {
    select_datafile(FileType.training)
  }

  override def select_testing_datafile(event: ActionEvent) = {
    select_datafile(FileType.testing)
  }

  def select_datafile(fileType: FileType) = {
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Import File")
    fileChooser.getExtensionFilters.addAll(
      new ExtensionFilter("Data Files", "*.dat")
    )
    fileChooser.showOpenMultipleDialog(MonitorApplication.getStage) match {
      case files: List[File] if files != null => pendingFileItems.addAll(files.map(file=>(file,fileType)).asJava)
        if (!handlingFile) handleNextFile()
      case _ =>
    }
  }

  def handleNextFile() = {
    val fileItem = pendingFileItems.poll()
    if (fileItem != null) {
      val file=fileItem._1
      val fileType=fileItem._2
      UIActor ! new ImportFile(fileType,file.getName, Source.fromFile(file).getLines().toSeq)
    }
  }

  def updated_right_status(statusType: ActionStatusType) = {
    right_status.setText(statusType.toString)
  }
}
