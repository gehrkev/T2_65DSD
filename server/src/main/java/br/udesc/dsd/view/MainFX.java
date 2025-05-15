package br.udesc.dsd.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Interface simples para teste
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Simulador de Tráfego");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
