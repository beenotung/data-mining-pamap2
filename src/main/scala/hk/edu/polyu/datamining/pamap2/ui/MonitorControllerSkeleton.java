package hk.edu.polyu.datamining.pamap2.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by beenotung on 1/30/16.
 */
public class MonitorControllerSkeleton {
  @FXML // ResourceBundle that was given to the FXMLLoader
  protected ResourceBundle resources;

  @FXML // URL location of the FXML file that was given to the FXMLLoader
  protected URL location;

  @FXML // fx:id="status_left"
  protected Label status_left; // Value injected by FXMLLoader

  @FXML
  void select_import_file(ActionEvent event) {

  }

  @FXML
    // This method is called by the FXMLLoader when initialization is complete
  void initialize() {
    assert status_left != null : "fx:id=\"status_left\" was not injected: check your FXML file 'MonitorApplication.fxml'.";

  }
}
