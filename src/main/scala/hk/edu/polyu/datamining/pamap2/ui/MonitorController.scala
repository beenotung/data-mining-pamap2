package hk.edu.polyu.datamining.pamap2.ui

import javafx.event.ActionEvent
import javafx.stage.FileChooser
import javafx.stage.FileChooser.ExtensionFilter

import hk.edu.polyu.datamining.pamap2.actor.UIActor
import hk.edu.polyu.datamining.pamap2.utils.Lang.consumer

/**
  * Created by beenotung on 1/30/16.
  */
class MonitorController extends MonitorControllerSkeleton {
  override def select_import_file(event: ActionEvent) = {
    val fileChooser = new FileChooser()
    fileChooser.setTitle("Import File")
    fileChooser.getExtensionFilters.addAll(
      new ExtensionFilter("Data Files", "*.dat")
    )
    val files = fileChooser.showOpenMultipleDialog(MonitorApplication.getStage)
    files.forEach(consumer(file => {
      UIActor.instance.self ! new UIActor.ImportFile(file)
    }))
  }
}
