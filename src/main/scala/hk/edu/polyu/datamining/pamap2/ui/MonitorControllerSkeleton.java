package hk.edu.polyu.datamining.pamap2.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

  @FXML // fx:id="left_status"
  protected Label left_status; // Value injected by FXMLLoader

  @FXML // fx:id="right_status"
  protected Button right_status; // Value injected by FXMLLoader

  @FXML
  void select_import_file(ActionEvent event) {

  }

  @FXML
  void update_right_status(ActionEvent event) {

  }

  @FXML
    // This method is called by the FXMLLoader when initialization is complete
  void initialize() {
    assert left_status != null : "fx:id=\"left_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert right_status != null : "fx:id=\"right_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    customInit();
  }

  protected void customInit() {
    System.out.println("customInit is not implemented");
  }
}
