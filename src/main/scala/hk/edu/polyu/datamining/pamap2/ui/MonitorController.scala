package hk.edu.polyu.datamining.pamap2.ui

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.{util => ju}
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import hk.edu.polyu.datamining.pamap2.actor.ActionState.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.DispatchActor.DispatchTask
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor.{DispatchActor, StateActor, UIActor}
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.ui.MonitorController._
import hk.edu.polyu.datamining.pamap2.utils.FileUtils
import hk.edu.polyu.datamining.pamap2.utils.Lang.{fork, runnable}

import scala.collection.JavaConverters._
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


  def importedFile(filename: String): Unit = Platform runLater (() => {
    instance.left_status.setText(s"imported $filename")
  })

  def restarted(reason: String) = Platform runLater (() => {
    instance.promptRestarted(reason)
  })

  def updateStatus(status: ActionStatusType): Unit = Platform runLater (() => {
    instance.updated_right_status(status)
  })

  def runOnUIThread(fun: () => Unit) = Platform runLater fun

  def setFileProgressText(value: String) = Platform runLater (() => {
    instance.file_progress_text.setText(value)
  })

  def setProgress(value: Double) = runOnUIThread(() => {
    instance.import_file_progress.setProgress(value)
  })

  val aborted = new AtomicBoolean(false)
}

class MonitorController extends MonitorControllerSkeleton {
  instance = this
  var pendingFileItems = new ConcurrentLinkedQueue[(File, FileType)]
  //  var handlingFile = new AtomicBoolean(false)

  def setUIStatus(msg: String) = {
    println(msg)
    left_status.setText(msg)
  }

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

  override def abort_import_datafile(event: ActionEvent) = {

  }

  def select_datafile(fileType: FileType) = {
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Import File")
    fileChooser.getExtensionFilters.addAll(
      new ExtensionFilter("Data Files", "*.dat")
    )
    fileChooser.showOpenMultipleDialog(MonitorApplication.getStage) match {
      case list: ju.List[File] if list != null =>
        val files = list.asScala
        setUIStatus(s"selected ${files.length} file(s)")
        pendingFileItems.addAll(files.map(file => (file, fileType)).asJava)
        handleNextFile()
      case _ =>
        setUIStatus("selected no files")
    }
  }

  def handleNextFile(): Unit = {
    fork(() => {
      val hasNext = pendingFileItems.synchronized[Boolean]({
        val numberOfFile = pendingFileItems.size()
        if (numberOfFile > 0) {
          setFileProgressText(s"$numberOfFile file(s) pending")
          val fileItem = pendingFileItems.poll()
          if (fileItem != null) {
            /**
              * 1. tell UI doing
              * 2. save to database
              * 3. tell global dispatcher
              * 4. tell UI done
              **/
            val file = fileItem._1
            val fileType = fileItem._2

            /* 1. tell UI doing */
            val filename: String = file.getName
            importingFile(filename)
            /* 2. save to database */
            val N = FileUtils.lineCount(file) / DatabaseHelper.BestInsertCount
            var i = 0f
            Source.fromFile(file).getLines().grouped(DatabaseHelper.BestInsertCount)
              .foreach(lines => {
                setProgress(i / N)
                DatabaseHelper.addRawDataFile(filename, lines, fileType)
                i += 1
              })
            setProgress(0)
            /* 3. tell global dispatcher */
            UIActor ! new DispatchTask(DispatchActor.Task.extractFromRawLine)
            /* 4. tell UI done */
            importedFile(filename)
            true
          }
          else
            false
        } else {
          setFileProgressText("all imported")
          false
        }
      })
      if (hasNext)
        handleNextFile()
    })
  }

  def updated_right_status(statusType: ActionStatusType) = {
    right_status.setText(statusType.toString)
  }
}
