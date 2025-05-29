package br.udesc.dsd.view;

import br.udesc.dsd.controller.ISimulacaoController;
import br.udesc.dsd.controller.MalhaController;
import br.udesc.dsd.factory.ControllerFactory;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainFX extends Application {
    private ISimulacaoController controllerAtual;
    private MalhaController malhaController;
    private String caminhoArquivo;
    private boolean usarMonitor = false;
    private MalhaView malhaView;

    @Override
    public void start(Stage primaryStage) {
        malhaView = new MalhaView(null);
        configurarBotoes();

        Scene scene = new Scene(malhaView.getRoot());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Tráfego");
        primaryStage.show();
    }

    private void configurarBotoes() {
        malhaView.iniciarBotao.setOnAction(e -> {
            if (controllerAtual != null) {
                controllerAtual.encerrarSimulacao();
                controllerAtual = null;
            }

            malhaView.getRoot().lookupAll(".grid-pane").forEach(node -> {
                if (node instanceof GridPane grid) {
                    grid.getChildren().clear();
                }
            });

            PauseTransition pausa = new PauseTransition(Duration.seconds(1));
            pausa.setOnFinished(event -> {
                String arquivoSelecionado = malhaView.comboBoxMalhas.getValue();
                if (arquivoSelecionado == null) {
                    exibirAlerta("Aviso", "Selecione um arquivo de malha antes de iniciar!");
                    return;
                }

                caminhoArquivo = getClass().getClassLoader().getResource(arquivoSelecionado).getPath();
                malhaController = new MalhaController(caminhoArquivo);

                try {
                    malhaView.inicializarMalha(malhaController.getMalha());

                    ISimulacaoController novoController = ControllerFactory
                            .criarSimulacaoController(malhaController, malhaView, usarMonitor);
                    controllerAtual = novoController;
                    controllerAtual.iniciarSimulacao();
                } catch (UnsupportedOperationException ex) {
                    System.err.println("Erro ao criar controller: " + ex.getMessage());
                    controllerAtual = null;
                }
            });

            pausa.play();
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

        malhaView.monitorRadio.setOnAction(e -> usarMonitor = true);
        malhaView.semaforoRadio.setOnAction(e -> usarMonitor = false);
    }


    private void exibirAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
