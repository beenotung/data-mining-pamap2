package hk.edu.polyu.datamining.pamap2.ui

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.{util => ju}
import javafx.application.Platform
import javafx.application.Platform.{runLater => runOnUIThread}
import javafx.event.ActionEvent
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout.VBox
import javafx.stage.{FileChooser, Popup}
import javafx.stage.FileChooser.ExtensionFilter

import com.rethinkdb.RethinkDB
import com.rethinkdb.model.MapObject
import com.rethinkdb.net.Cursor
import hk.edu.polyu.datamining.pamap2.Main
import hk.edu.polyu.datamining.pamap2.actor.ActionStatus.ActionStatusType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType
import hk.edu.polyu.datamining.pamap2.actor.ImportActor.FileType.FileType
import hk.edu.polyu.datamining.pamap2.actor.MessageProtocol.{ClusterComputeInfo, RequestClusterComputeInfo, StartARM}
import hk.edu.polyu.datamining.pamap2.actor._
import hk.edu.polyu.datamining.pamap2.database.{DatabaseHelper, Tables}
import hk.edu.polyu.datamining.pamap2.ui.MonitorController._
import hk.edu.polyu.datamining.pamap2.utils.FormatUtils.formatSize
import hk.edu.polyu.datamining.pamap2.utils.Lang._
import hk.edu.polyu.datamining.pamap2.utils.{FileUtils, Log}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Created by beenotung on 1/30/16.
  */
object MonitorController {
  val aborted = new AtomicBoolean(false)
  //  val autoUpdate = Main.config.getBoolean("ui.autoupdate.enable")
  val interval = Main.config.getInt("ui.autoupdate.interval")
  var clusterComputeInfo: ClusterComputeInfo = (0L, IndexedSeq.empty)
  private[ui] var instance: MonitorController = null

  def receivedNodeInfos(newVal: ClusterComputeInfo) = {
    //    Log.info("received nodeinfos")
    clusterComputeInfo = newVal
    val status = DatabaseHelper.getActionStatus
    runOnUIThread(() => {
      instance.updated_computeNodeInfos()
      instance.cluster_status.setText(status.toString)
    })
    NodesDetailController.updateList()
    //TODO use UIActor scheduler
    if (instance.auto_update.isSelected)
      fork(() => {
        Thread.sleep(interval)
        /* ask for cluster status */
        UIActor.dispatch(RequestClusterComputeInfo)
      })
  }

  def onActorSystemTerminated() = if (instance != null) runOnUIThread(() => {
    val alert = new Alert(AlertType.ERROR)
    alert.setTitle("Error")
    alert.setHeaderText("Actor System is shutdown")
    alert.setContentText("check the console for more detail\ne.g. not implemented error (???)")
    alert.showAndWait()
    Platform.exit()
  })

  def setLeftStatus(s: String) = runOnUIThread(() => instance.left_status.setText(s))

  def restarted(reason: String) = Platform runLater (() => {
    instance.promptRestarted(reason)
  })

  def setFileProgressText(value: String) = Platform runLater (() => {
    instance.file_progress_text.setText(value)
  })

  def setImportProgress(value: Double) = runOnUIThread(() => {
    instance.import_file_progress.setProgress(value)
  })

  def setActionStatus(actionStatusType: ActionStatusType) = {
    fork(runnable(() => DatabaseHelper.setActionStatus(actionStatusType)))
    runOnUIThread(() => instance.cluster_status.setText(actionStatusType.toString))
  }
}

class MonitorController extends MonitorControllerSkeleton {
  instance = this
  val cursorRef = new AtomicReference[Cursor[ju.Map[String, ju.Map[String, AnyRef]]]](null)
  var pendingFileItems = new ConcurrentLinkedQueue[(File, FileType, Double)]

  def setUIStatus(msg: String) = {
    Log.info(msg)
    left_status.setText(msg)
  }

  override def customInit() = {
    /* get cluster status */
    update_cluster_status(new ActionEvent())

    /* get cluster compute info */
    auto_update.setSelected(Main.config.getBoolean("ui.autoupdate.enable"))
    auto_update_onChanged(new ActionEvent())
  }

  override def show_ip_list(event: ActionEvent) = {
    fork(() => {
      val ips = DatabaseHelper.runToBuffer[String](_.table(Tables.ClusterSeed.name).getField(Tables.ClusterSeed.Field.host.toString))
      runOnUIThread(() => {
        val popup = new Popup()
        //        popup.setAutoFix(false)
        //        popup.setHideOnEscape(true)
        val vbox = new VBox()
        val textField = new TextField()
        textField.setText(ips.reduce((a, b) => s"$a $b"))
        vbox.getChildren.add(textField)
        popup.getContent.add(vbox)
        val close = new Button("Close")
        close.setOnAction(eventHandler(_ => popup.hide()))
        popup.getContent.add(close)
        popup.show(MonitorApplication.getStage)
      })
    })
  }

  override def auto_update_onChanged(event: ActionEvent) = {
    UIActor.dispatch(RequestClusterComputeInfo)
  }

  override def update_cluster_status(event: ActionEvent) = {
    val msg: String = "getting cluster status"
    //println(msg)
    cluster_status.setText(msg)
    /* set ui to loading */
    val loading = "loading"
    btn_nodes setText loading
    text_cluster_processor setText loading
    text_cluster_memory setText loading
    text_number_of_pending_task setText loading
    text_number_of_completed_task setText loading
    fork(() => {
      /* ask for cluster status */
      UIActor.dispatch(RequestClusterComputeInfo)
      /* get staus from database */
      val status = DatabaseHelper.getActionStatus
      runOnUIThread(() => cluster_status setText status.toString)
    })
  }

  def promptRestarted(reason: String): Unit = {
    val alert = new Alert(AlertType.WARNING)
    alert.setTitle("Warning")
    alert.setHeaderText("Service Restarted")
    alert.setContentText(reason)
    alert.showAndWait()
    left_status.setText("")
  }

  override def select_subject_datafile(event: ActionEvent) = {
    select_datafile(FileType.subject, "csv", "Select Subject data file")
  }

  override def select_training_datafile(event: ActionEvent) = {
    select_datafile(FileType.training, "dat", "Select Traning data file")
  }

  override def select_testing_datafile(event: ActionEvent) = {
    select_datafile(FileType.testing, "dat", "Select Testing data file")
  }

  override def abort_import_datafile(event: ActionEvent) = {
    MonitorController.aborted.set(true)
  }

  override def show_nodes_detail(event: ActionEvent) = {
    NodesDetailController.start()
  }

  override def reset_subject_train_test_data(event: ActionEvent) = {
    val alert = new Alert(AlertType.CONFIRMATION)
    alert.setTitle("Confirmation")
    alert.setHeaderText("Reset subject profile, training data and testing data")
    alert.setContentText("Are you sure to reset the data?")
    alert.showAndWait().get() match {
      case ButtonType.OK =>
        refresh_dataset_count_progress setProgress -1
        /* fork to do network stuff */
        fork(runnable(() => {
          DatabaseHelper.createTableDropIfExistResult(Tables.Subject.name)
          runOnUIThread(() => {
            subject_count setText 0.toString
            refresh_dataset_count_progress setProgress 0.5
          })
          DatabaseHelper.createTableDropIfExistResult(Tables.RawData.name)
          runOnUIThread(() => {
            training_data_count setText 0.toString
            testing_data_count setText 0.toString
            refresh_dataset_count_progress setProgress 1
          })
          setActionStatus(ActionStatus.init)
        }))
      case _ =>
    }
  }

  override def update_dataset_count(event: ActionEvent) = {
    val msg: String = "refreshing dataset count"
    //println(msg)
    left_status setText msg
    subject_count setText "loading"
    training_data_count setText "loading"
    testing_data_count setText "loading"
    refresh_dataset_count_progress.setProgress(-1)

    /* fork to do network stuff */
    fork(() => {
      /*  get content from database  */

      /* get subject count */
      val subjectCount: Long = DatabaseHelper.run(_.table(Tables.Subject.name).count())
      runOnUIThread(() => {
        subject_count.setText(subjectCount.toString)
        refresh_dataset_count_progress setProgress 1d / 3
      })

      val table = Tables.RawData
      val field = Tables.RawData.Field
      /* get test count */
      val testCount: Long = DatabaseHelper.run(_.table(table.name).withFields(field.isTest.toString).count())
      runOnUIThread(() => {
        testing_data_count.setText(testCount.toString)
        refresh_dataset_count_progress setProgress 2d / 3
      })

      /* get train count */
      val trainCount: Long = DatabaseHelper.run(_.table(table.name).withFields(field.isTrain.toString).count())
      runOnUIThread(() => {
        /* show content to ui */
        training_data_count.setText(trainCount.toString)
        left_status setText "refreshed dateset count"
        refresh_dataset_count_progress setProgress 1
      })
    })
  }

  override def reset_cluster_db(event: ActionEvent) = {
    val alert = new Alert(AlertType.WARNING)
    alert.setTitle("Warning")
    alert.setHeaderText("Reset Cluster Status")
    alert.setContentText(s"cluster seed, task will be erased!")
    alert.showAndWait().get() match {
      case ButtonType.OK =>
        Seq(Tables.ClusterSeed.name, Tables.Task.name).foreach(DatabaseHelper.createTableDropIfExistResult)
        UIActor.dispatch(MessageProtocol.RestartCluster)

      case _ =>
    }
  }

  override def start_association_rule_mining(event: ActionEvent) = {
    try {
      val start = min_support_start.getText.toDouble
      val end = min_support_end.getText.toDouble
      val step = min_support_step.getText.toDouble
      val percentage = percentage_training_data.getText.toDouble / 100d
      val iterCount = Math.ceil((end - start) / step)
      val totalCount: Long = DatabaseHelper.run(_.table(Tables.RawData.name).hasFields(Tables.RawData.Field.isTrain.toString).count())
      val sampleCount = Math.round(totalCount * percentage)
      Log.info(s"start arm, found $iterCount iteration(s), $totalCount sample(s) will be used")
      if (iterCount <= 0) {
        val alert = new Alert(AlertType.ERROR)
        alert.setTitle("Error")
        alert.setHeaderText("Invalid Parameter")
        alert.setContentText(s"$iterCount iteration, wrong direction?")
        alert.showAndWait()
      } else if (percentage <= 0 || percentage > 1) {
        val alert = new Alert(AlertType.ERROR)
        alert.setTitle("Error")
        alert.setHeaderText("Invalid Parameter")
        alert.setContentText(s"percentage should be (0..100]")
        alert.showAndWait()
      }
      else if (sampleCount == 0) {
        val alert = new Alert(AlertType.ERROR)
        alert.setTitle("Error")
        alert.setHeaderText("Too low sampling rate")
        alert.setContentText(s"No training data is sampled at all")
        alert.showAndWait()
      }
      else
        UIActor.dispatch(StartARM(percentage, start, end, step))
    } catch {
      case e: NumberFormatException =>
        val alert = new Alert(AlertType.ERROR)
        alert.setTitle("Error")
        alert.setHeaderText("Invalid Parameter")
        alert.setContentText(s"please input real number\n$e")
        alert.showAndWait()
    }
  }

  def select_datafile(fileType: FileType, extension: String = "dat", title: String = "Import File") = {
    val sampleRate = import_sample_rate.getText.toDouble / 100
    if (sampleRate <= 0 || sampleRate > 1) {
      val alert = new Alert(AlertType.ERROR)
      alert.setTitle("Error")
      alert.setHeaderText("Invalid Parameter")
      alert.setContentText("The import sample rate should be larger than zero and smaller than or equal to 100")
    }
    val fileChooser = new FileChooser()
    fileChooser.setTitle(title)
    fileChooser.getExtensionFilters.addAll(
      new ExtensionFilter("Data Files", s"*.$extension")
    )
    fileChooser.showOpenMultipleDialog(MonitorApplication.getStage) match {
      case list: ju.List[File] if list != null =>
        val files = list.asScala
        setUIStatus(s"selected ${files.length} file(s)")
        pendingFileItems.addAll(files.map(file => (file, fileType, sampleRate)).asJava)
        handleNextFile()
      case _ =>
        setUIStatus("selected no files")
    }
  }

  def handleNextFile(numberOfFileDone: Int = 0): Unit = fork(() => {
    if (numberOfFileDone == 0) {
      //println("set action status to importing")
      setActionStatus(ActionStatus.importing)
    }
    pendingFileItems.synchronized({
      val numberOfFile = pendingFileItems.size()
      if (numberOfFile > 0) {
        setLeftStatus(s"finished $numberOfFileDone file(s)")
        // handle one now
        val fileItem = pendingFileItems.poll()
        if (fileItem == null)
          return
        val (file, filetype, sampleRate) = fileItem
        val filename = file.getName
        val (table, fileTypeField) = filetype match {
          case FileType.subject => (Tables.Subject.name, "")
          case FileType.training => (Tables.RawData.name, Tables.RawData.Field.isTrain.toString)
          case FileType.testing => (Tables.RawData.name, Tables.RawData.Field.isTest.toString)
        }
        setFileProgressText(s"importing $filename into $table ${if (fileTypeField.length > 0) s"($fileTypeField)"}")
        setImportProgress(0)
        val N = FileUtils.lineCount(file)
        var i = 0f
        if (filetype.equals(FileType.subject)) {
          // subject data
          val lines: Iterator[String] = Source.fromFile(file).getLines()
          val titles = lines.take(1).toIndexedSeq.head.split(",")
          lines.grouped(DatabaseHelper.BestInsertCount)
            .takeWhile(_ => !aborted.get())
            .foreach(lines => {
              setImportProgress(i / N)
              val rows = lines.map(line => ImportActor.processSubject(titles, line))
              DatabaseHelper.tableInsertRows(table, rows, softDurability = true)
              i += lines.size
            })
        } else {
          // training or testing data
          val subject = filename.split("\\.")(0).filter(c => c >= '0' && c <= '9')
          val subjectField = Tables.RawData.Field.subject.toString
          val activityBuffer = mutable.ListBuffer.empty[MapObject]
          val activity_f = Tables.RawData.Field.activityId.toString
          val subject_f = Tables.RawData.Field.subject.toString
          val timeSequence_f = Tables.RawData.Field.timeSequence.toString
          var lastActivityId: Byte = -1
          Source.fromFile(file).getLines()
            .takeWhile(_ => !aborted.get())
            .foreach(line => {
              i += 1
              val (activityId, activitySlice) = ImportActor.processActivitySlice(line)
              if (activityId != 0)
                if (lastActivityId == activityId && activityBuffer.length < DatabaseHelper.Max_Row) {
                  activityBuffer += activitySlice
                } else {
                  setImportProgress(i / N)
                  if (activityBuffer.nonEmpty && Random.nextDouble() < sampleRate) {
                    /* insert to database */
                    val row = RethinkDB.r.hashMap(activity_f, lastActivityId)
                      .`with`(subject_f, subject)
                      .`with`(fileTypeField.toString, true)
                      .`with`(timeSequence_f, activityBuffer.toList.asJava)
                    DatabaseHelper.tableInsertRow(table, row) //, softDurability = true)
                  }
                  lastActivityId = activityId
                  activityBuffer.clear()
                  activityBuffer += activitySlice
                }
            })
          if (activityBuffer.nonEmpty) {
            val row = RethinkDB.r.hashMap(activity_f, lastActivityId)
              .`with`(subject_f, subject)
              .`with`(fileTypeField.toString, true)
              .`with`(timeSequence_f, activityBuffer.toList.asJava)
            DatabaseHelper.tableInsertRow(table, row) //, softDurability = true)
            activityBuffer.clear()
          }
        }
        setImportProgress(1)
        setLeftStatus(s"finished $filename")
        if (numberOfFile > 1)
          handleNextFile(numberOfFileDone + 1)
        else {
          //println("set action status to imported")
          setActionStatus(ActionStatus.imported)
          setFileProgressText("all imported")
          aborted.set(false)
        }
      }
    })
  })

  def updated_computeNodeInfos() = {
    if (clusterComputeInfo._2.isEmpty) {
      def setText(x: Labeled) = {
        x setText "none"
      }
      setText(btn_nodes)
      setText(text_cluster_processor)
      setText(text_cluster_memory)
      setText(text_number_of_pending_task)
      setText(text_number_of_completed_task)
    } else {
      btn_nodes setText clusterComputeInfo._2.size.toString
      val nodeInfos = clusterComputeInfo._2.map(_.nodeInfo)
      text_cluster_processor setText clusterComputeInfo._2.map(_.workerRecords.length).sum.toString
      text_cluster_memory setText {
        val total = nodeInfos.map(_.totalMemory).sum
        val free = nodeInfos.map(_.freeMemory).sum
        val max = nodeInfos.map(_.maxMemory).sum
        val used: Long = total - free
        val usage = 100 * used / max
        s"${formatSize(used)} / ${formatSize(max)} ($usage%)"
      }
      val workerRecords = clusterComputeInfo._2.flatMap(_.workerRecords).toIndexedSeq
      text_number_of_pending_task setText workerRecords.map(_.pendingTask).sum.toString + "/" + clusterComputeInfo._1
      text_number_of_completed_task setText workerRecords.map(_.completedTask).sum.toString
    }
  }
}