package org.example.lbycpds_project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/login/Login_View.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("Electronics Inventory System");
        stage.setScene(scene);

        stage.sizeToScene();
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}