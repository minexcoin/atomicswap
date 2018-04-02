package com.minexcoin.atomic_swap.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class App extends Application {
	@Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/HomePage.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("MinexExchange");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
