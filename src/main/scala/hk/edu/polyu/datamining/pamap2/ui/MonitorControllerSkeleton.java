package hk.edu.polyu.datamining.pamap2.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

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

  @FXML // fx:id="min_support_start"
  protected TextField min_support_start; // Value injected by FXMLLoader

  @FXML // fx:id="min_support_end"
  protected TextField min_support_end; // Value injected by FXMLLoader

  @FXML // fx:id="min_support_start_step"
  protected TextField min_support_start_step; // Value injected by FXMLLoader

  @FXML // fx:id="left_status"
  protected Label left_status; // Value injected by FXMLLoader

  @FXML // fx:id="right_status"
  protected Button right_status; // Value injected by FXMLLoader

  @FXML
  void select_testing_datafile(ActionEvent event) {

  }

  @FXML
  void select_training_datafile(ActionEvent event) {

  }

  @FXML
  void start_association_rule_mining(ActionEvent event) {

  }

  @FXML
  void update_right_status(ActionEvent event) {

  }

  @FXML
    // This method is called by the FXMLLoader when initialization is complete
  void initialize() {
    assert min_support_start != null : "fx:id=\"min_support_start\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert min_support_end != null : "fx:id=\"min_support_end\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert min_support_start_step != null : "fx:id=\"min_support_start_step\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert left_status != null : "fx:id=\"left_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert right_status != null : "fx:id=\"right_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    customInit();
  }

  protected void customInit() {
    System.out.println("customInit is not implemented");
  }
}
