package br.udesc.dsd.view;

import br.udesc.dsd.controller.ISimulacaoController;
import br.udesc.dsd.controller.MalhaController;
import br.udesc.dsd.factory.ControllerFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {
    private ISimulacaoController controllerAtual;
    private MalhaController malhaController;
    private String caminhoArquivo;
    private boolean usarMonitor = false;
    private MalhaView malhaView;

    @Override
    public void start(Stage primaryStage) {
        caminhoArquivo = getClass().getClassLoader().getResource("malha-exemplo-3.txt").getPath();
        malhaController = new MalhaController(caminhoArquivo);

        malhaView = new MalhaView(malhaController.getMalha());

        usarMonitor = malhaView.monitorRadio.isSelected();

        configurarBotoes();

        Scene scene = new Scene(malhaView.getRoot());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Tráfego");
        primaryStage.show();
    }

    private void configurarBotoes() {

        malhaView.iniciarBotao.setOnAction(e -> {
            malhaController = new MalhaController(caminhoArquivo);
            malhaView.setMalha(malhaController.getMalha());

            try {
                ISimulacaoController novoController = ControllerFactory
                        .criarSimulacaoController(malhaController, malhaView, usarMonitor);
                controllerAtual = novoController;
                controllerAtual.iniciarSimulacao();
            } catch (UnsupportedOperationException ex) {
                System.err.println("Erro ao criar controller: " + ex.getMessage());
                controllerAtual = null;
            }
        });

        malhaView.encerrarInsercaoBotao.setOnAction(e -> {
            if (controllerAtual != null) {
                controllerAtual.pararInsercao();
            }
        });

        malhaView.encerrarSimulacaoBotao.setOnAction(e -> {
            if (controllerAtual != null) {
                controllerAtual.encerrarSimulacao();
                controllerAtual = null;
            }
        });

        malhaView.monitorRadio.setOnAction(e -> {
            usarMonitor = true;
        });

        malhaView.semaforoRadio.setOnAction(e -> {
            usarMonitor = false;
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}