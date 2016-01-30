package hk.edu.polyu.datamining.pamap2.ui;/**
 * Created by beenotung on 1/30/16.
 */

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MonitorApplication extends Application {
  public static Stage stage = null;

  public static Stage getStage() {
    return stage;
  }

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws IOException {
    MonitorApplication.stage = stage;
    Parent root = FXMLLoader.load(getClass().getResource("MonitorApplication.fxml"));
    Scene scene = new Scene(root);
    Config config = ConfigFactory.parseResources("node.ui.conf");
    stage.setTitle(config.getString("ui.title"));
    stage.setScene(scene);
    stage.show();
  }
}
