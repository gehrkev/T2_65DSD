package br.udesc.dsd.view;

import br.udesc.dsd.controller.MalhaController;
import br.udesc.dsd.model.Carro;
import br.udesc.dsd.model.Quadrante;
import br.udesc.dsd.view.MalhaView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainFX extends Application {

    private MalhaController controller;
    private MalhaView malhaView;

    @Override
    public void start(Stage primaryStage) {
        // Carrega a malha do arquivo
        String caminhoArquivo = getClass().getClassLoader().getResource("malha-exemplo-3.txt").getPath();
        controller = new MalhaController(caminhoArquivo);

        // Cria a view da malha com base no controller
        malhaView = new MalhaView(controller.getMalha());

        // Cria cena e exibe
        VBox root = new VBox(malhaView.getGrid());
        Scene scene = new Scene(root);
        primaryStage.setTitle("Simulador de Tráfego");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Insere carro e inicia simulação
        inserirCarro(800, 2);
        inserirCarro(800, 0);
    }

    private void inserirCarro(int velocidade, int entr) {
        if (controller.getPontosDeEntrada().isEmpty()) return;

        Quadrante entrada = controller.getPontosDeEntrada().get(entr);
        System.out.println("Entrada linha: " + entrada.getLinha());
        System.out.println("Entrada col: " + entrada.getColuna());

        Carro carro = new Carro(entrada, velocidade, malhaView); // Versão com referência à MalhaView
        carro.setName("Carro-1");

        entrada.adicionarCarro(carro);
        entrada.setQuadranteDoCarro();
        Platform.runLater(malhaView::atualizarCelulas);

        carro.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
