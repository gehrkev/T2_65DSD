package br.udesc.dsd.view;

import br.udesc.dsd.controller.MalhaController;
import br.udesc.dsd.controller.SimulacaoController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        String caminhoArquivo = getClass().getClassLoader().getResource("malha-exemplo-3.txt").getPath();
        MalhaController malhaController = new MalhaController(caminhoArquivo);

        MalhaView malhaView = new MalhaView(malhaController.getMalha());
        SimulacaoController simulacaoController = new SimulacaoController(malhaController, malhaView);

        Scene scene = new Scene(malhaView.getRoot());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Tráfego");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
