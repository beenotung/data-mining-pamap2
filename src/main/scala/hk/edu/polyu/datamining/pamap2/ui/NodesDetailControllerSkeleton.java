package hk.edu.polyu.datamining.pamap2.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by beenotung on 2/10/16.
 */
public class NodesDetailControllerSkeleton {
  @FXML // ResourceBundle that was given to the FXMLLoader
  protected ResourceBundle resources;

  @FXML // URL location of the FXML file that was given to the FXMLLoader
  protected URL location;

  @FXML // fx:id="main_vbox"
  protected VBox main_vbox; // Value injected by FXMLLoader

  @FXML // fx:id="messageLabel"
  protected Label messageLabel; // Value injected by FXMLLoader

  @FXML // fx:id="detailsLabel"
  protected Label detailsLabel; // Value injected by FXMLLoader

  @FXML
  void close_window(ActionEvent event) {

  }

  @FXML
    // This method is called by the FXMLLoader when initialization is complete
  void initialize() {
    assert main_vbox != null : "fx:id=\"main_vbox\" was not injected: check your FXML file 'NodesDetail.fxml'.";
    assert messageLabel != null : "fx:id=\"messageLabel\" was not injected: check your FXML file 'NodesDetail.fxml'.";
    assert detailsLabel != null : "fx:id=\"detailsLabel\" was not injected: check your FXML file 'NodesDetail.fxml'.";
    customInit();
  }

  protected void customInit() {
    System.out.println("customInit is not implemented");
  }
}
