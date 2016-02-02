package hk.edu.polyu.datamining.pamap2.ui.association_rule

import javafx.geometry.Insets
import javafx.scene.control._
import javafx.scene.layout.StackPane
import javafx.stage.Stage

import hk.edu.polyu.datamining.pamap2.utils.Lang._

/**
  * Created by beenotung on 2/2/16.
  */
class ProcessCell(val stage: Stage, val table: TableView[hk.edu.polyu.datamining.pamap2.association_rule.Process])
  extends TableCell[hk.edu.polyu.datamining.pamap2.association_rule.Process, hk.edu.polyu.datamining.pamap2.association_rule.Process] {
  val onButton = new RadioButton("On")
  val offButton = new RadioButton("Off")
  val toggleGroup = new ToggleGroup()
  val buttonBar = new ButtonBar()
  val pane = new StackPane()

  /* set layout */
  pane.setPadding(new Insets(3))
  pane.getChildren.add(buttonBar)
  buttonBar.getButtons.add(onButton)
  buttonBar.getButtons.add(offButton)

  /* set radio button relation ship */
  onButton.setToggleGroup(toggleGroup)
  offButton.setToggleGroup(toggleGroup)

  /* set event listener */
  def onValueChanged(on: Boolean) = {
    if (on)
      onButton.setSelected(true)
    else
      offButton.setSelected(true)
  }

  onButton.setOnAction(eventHandler(_ => onValueChanged(true)))
  offButton.setOnAction(eventHandler(_ => onValueChanged(false)))

  //  override def updateItem(item:Boolean,empty:Boolean)={}
  /** places an add button in the row only if the row is not empty. */
  override protected def updateItem(item: hk.edu.polyu.datamining.pamap2.association_rule.Process, empty: Boolean) {
    super.updateItem(item, empty)
    /* add to ui */
    if (!empty) {
      onValueChanged(item.isAutoProperty.get())
      setContentDisplay(ContentDisplay.GRAPHIC_ONLY)
      setGraphic(pane)
    }
    else {
      setGraphic(null)
    }
  }
}
