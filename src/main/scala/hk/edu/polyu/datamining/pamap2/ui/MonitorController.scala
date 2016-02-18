package hk.edu.polyu.datamining.pamap2.ui

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.{util => ju}
import javafx.application.Platform
import javafx.application.Platform.{runLater => runOnUIThread}
import javafx.event.ActionEvent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import akka.cluster.MemberStatus
import hk.edu.polyu.datamining.pamap2.actor.ActionState.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor._
import hk.edu.polyu.datamining.pamap2.database.DatabaseHelper
import hk.edu.polyu.datamining.pamap2.ui.MonitorController._
import hk.edu.polyu.datamining.pamap2.utils.FileUtils
import hk.edu.polyu.datamining.pamap2.utils.Lang._

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * Created by beenotung on 1/30/16.
  */
object MonitorController {
  val aborted = new AtomicBoolean(false)
  var nodes = Set.empty[NodeInfo]
  var nodes_num = UIActor.members.size
  private[ui] var instance: MonitorController = null

  def receivedNodeInfos(newNodes: Seq[NodeInfo]) = nodes = newNodes.toSet


  def onActorSystemTerminated() = if (instance != null) runOnUIThread(() => {
    val alert = new Alert(AlertType.ERROR)
    alert.setTitle("Error")
    alert.setHeaderText("Actor System is shutdown")
    alert.setContentText("check the console for more detail\ne.g. not implemented error (???)")
    alert.showAndWait()
    Platform.exit()
  })

  def importingFile(filename: String) = Platform runLater (() => {
    instance.left_status.setText(s"importing $filename")
  })

  def importedFile(filename: String): Unit = Platform runLater (() => {
    instance.left_status.setText(s"imported $filename")
  })

  def restarted(reason: String) = Platform runLater (() => {
    instance.promptRestarted(reason)
  })

  def receivedClusterStatus(status: ActionStatusType): Unit = Platform runLater (() => {
    instance.updated_cluster_status(status)
  })

  def setFileProgressText(value: String) = Platform runLater (() => {
    instance.file_progress_text.setText(value)
  })

  def setProgress(value: Double) = runOnUIThread(() => {
    instance.import_file_progress.setProgress(value)
  })
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
    //update_cluster_info(new ActionEvent())

    /* update general cluster info */
    MonitorActor.addListener((event, members) => {
      val n = members.count(_.status == MemberStatus.Up)
      UIActor.requestUpdate()
      runOnUIThread(() => {
        btn_nodes.setText(n.toString)
      })
    })
  }

  override def update_cluster_info(event: ActionEvent) = {
    cluster_status.setText("getting cluster status")
    /* set ui to loading */
    val loading = "loading"
    btn_nodes setText loading
    text_cluster_processor setText loading
    text_cluster_memory setText loading
    text_number_of_pending_task setText loading
    text_number_of_completed_tasl setText loading
    /* ask for cluster status */
    UIActor.requestUpdate()
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
    MonitorController.aborted.set(true)
  }

  override def show_nodes_detail(event: ActionEvent) = {
    NodesDetailController.start()
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
            //TODO
            //            UIActor ! new DispatchTask(DispatchActorProtocol.Task.extractFromRawLine)
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
      /* check if abort is fired */
      if (aborted.get()) {
        /* user has abort the uploading, remove all files from pending queue */
        removeAll(pendingFileItems)
      } else {
        /* user has not abort the uploading, continue next file if exist */
        if (hasNext)
          handleNextFile()
      }
    })
  }

  def updated_cluster_status(statusType: ActionStatusType) = {
    cluster_status.setText(statusType.toString)
  }
}
