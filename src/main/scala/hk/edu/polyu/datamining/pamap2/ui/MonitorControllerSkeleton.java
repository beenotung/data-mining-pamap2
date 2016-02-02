package hk.edu.polyu.datamining.pamap2.ui;

import hk.edu.polyu.datamining.pamap2.association_rule.Process;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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

  @FXML // fx:id="association_rule_mining_process_table"
  protected TableView<Process> association_rule_mining_process_table; // Value injected by FXMLLoader

  @FXML // fx:id="association_rule_mining_process_table_process"
  protected TableColumn<Process, String> association_rule_mining_process_table_process; // Value injected by FXMLLoader

  @FXML // fx:id="association_rule_mining_process_table_auto"
  protected TableColumn<Process,Process > association_rule_mining_process_table_auto; // Value injected by FXMLLoader

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
    assert association_rule_mining_process_table != null : "fx:id=\"association_rule_mining_process_table\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert association_rule_mining_process_table_process != null : "fx:id=\"association_rule_mining_process_table_process\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert association_rule_mining_process_table_auto != null : "fx:id=\"association_rule_mining_process_table_auto\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert left_status != null : "fx:id=\"left_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert right_status != null : "fx:id=\"right_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    customInit();
  }

  protected void customInit() {
    System.out.println("customInit is not implemented");
  }
}
