package hk.edu.polyu.datamining.pamap2.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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

  @FXML // fx:id="text_cluster_processor"
  protected Label text_cluster_processor; // Value injected by FXMLLoader

  @FXML // fx:id="text_number_of_pending_task"
  protected Label text_number_of_pending_task; // Value injected by FXMLLoader

  @FXML // fx:id="text_number_of_completed_task"
  protected Label text_number_of_completed_task; // Value injected by FXMLLoader

  @FXML // fx:id="text_cluster_memory"
  protected Label text_cluster_memory; // Value injected by FXMLLoader

  @FXML // fx:id="btn_nodes"
  protected Button btn_nodes; // Value injected by FXMLLoader

  @FXML // fx:id="import_file_progress"
  protected ProgressBar import_file_progress; // Value injected by FXMLLoader

  @FXML // fx:id="file_progress_text"
  protected Label file_progress_text; // Value injected by FXMLLoader

  @FXML // fx:id="training_data_count"
  protected Label training_data_count; // Value injected by FXMLLoader

  @FXML // fx:id="testing_data_count"
  protected Label testing_data_count; // Value injected by FXMLLoader

  @FXML // fx:id="subject_count"
  protected Label subject_count; // Value injected by FXMLLoader

  @FXML // fx:id="refresh_dataset_count_progress"
  protected ProgressIndicator refresh_dataset_count_progress; // Value injected by FXMLLoader

  @FXML // fx:id="min_support_start"
  protected TextField min_support_start; // Value injected by FXMLLoader

  @FXML // fx:id="min_support_end"
  protected TextField min_support_end; // Value injected by FXMLLoader

  @FXML // fx:id="min_support_start_step"
  protected TextField min_support_start_step; // Value injected by FXMLLoader

  @FXML // fx:id="left_status"
  protected Label left_status; // Value injected by FXMLLoader

  @FXML // fx:id="auto_update"
  protected CheckBox auto_update; // Value injected by FXMLLoader

  @FXML // fx:id="cluster_status"
  protected Button cluster_status; // Value injected by FXMLLoader

  @FXML
  void abort_import_datafile(ActionEvent event) {

  }

  @FXML
  void auto_update_onChanged(ActionEvent event) {

  }

  @FXML
  void reset_subject_train_test_data(ActionEvent event) {

  }

  @FXML
  void select_subject_datafile(ActionEvent event) {

  }

  @FXML
  void select_testing_datafile(ActionEvent event) {

  }

  @FXML
  void select_training_datafile(ActionEvent event) {

  }

  @FXML
  void show_nodes_detail(ActionEvent event) {

  }

  @FXML
  void start_association_rule_mining(ActionEvent event) {

  }

  @FXML
  void update_cluster_status(ActionEvent event) {

  }

  @FXML
  void update_dataset_count(ActionEvent event) {

  }

  @FXML
    // This method is called by the FXMLLoader when initialization is complete
  void initialize() {
    assert text_cluster_processor != null : "fx:id=\"text_cluster_processor\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert text_number_of_pending_task != null : "fx:id=\"text_number_of_pending_task\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert text_number_of_completed_task != null : "fx:id=\"text_number_of_completed_task\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert text_cluster_memory != null : "fx:id=\"text_cluster_memory\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert btn_nodes != null : "fx:id=\"btn_nodes\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert import_file_progress != null : "fx:id=\"import_file_progress\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert file_progress_text != null : "fx:id=\"file_progress_text\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert training_data_count != null : "fx:id=\"training_data_count\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert testing_data_count != null : "fx:id=\"testing_data_count\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert subject_count != null : "fx:id=\"subject_count\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert refresh_dataset_count_progress != null : "fx:id=\"refresh_dataset_count_progress\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert min_support_start != null : "fx:id=\"min_support_start\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert min_support_end != null : "fx:id=\"min_support_end\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert min_support_start_step != null : "fx:id=\"min_support_start_step\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert left_status != null : "fx:id=\"left_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert auto_update != null : "fx:id=\"auto_update\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    assert cluster_status != null : "fx:id=\"cluster_status\" was not injected: check your FXML file 'MonitorApplication.fxml'.";
    customInit();
  }

  protected void customInit() {
    System.out.println("customInit is not implemented");
  }
}
