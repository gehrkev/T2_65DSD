package br.udesc.dsd.controller;

import br.udesc.dsd.model.Carro;
import br.udesc.dsd.model.Quadrante;
import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

public class SimulacaoController {

    private final MalhaController malhaController;
    private final MalhaView malhaView;

    private Thread threadInsercao;
    private final List<Carro> carrosAtivos = new ArrayList<>();
    private volatile boolean insercaoAtiva = false;

    public SimulacaoController(MalhaController malhaController, MalhaView malhaView) {
        this.malhaController = malhaController;
        this.malhaView = malhaView;

        configurarEventos();
    }

    private void configurarEventos() {
        malhaView.iniciarBotao.setOnAction(e -> iniciarSimulacao());
        malhaView.encerrarInsercaoBotao.setOnAction(e -> pararInsercao());
        malhaView.encerrarSimulacaoBotao.setOnAction(e -> encerrarSimulacao());
    }

    private void iniciarSimulacao() {
        Platform.runLater(() -> malhaView.exibirMensagemFinal(false));
        if (insercaoAtiva) return;

        int limite = malhaView.limiteVeiculosSpinner.getValue();
        int intervalo = malhaView.intervaloSpinner.getValue();
        boolean usarMonitor = malhaView.monitorRadio.isSelected();

        insercaoAtiva = true;

        threadInsercao = new Thread(() -> {
            try {
                while (insercaoAtiva) {
                    Platform.runLater(() -> {
                        if (carrosAtivos.size() < limite) {
                            int entradaIndex = (int) (Math.random() * malhaController.getPontosDeEntrada().size());
                            Carro carro = criarNovoCarro(entradaIndex);
                            carrosAtivos.add(carro);

                            // Quando o carro termina, remove da lista
                            carro.setOnTermino(() -> {
                                carrosAtivos.remove(carro);
                            });

                            carro.start();
                        }
                    });

                    Thread.sleep(intervalo);
                }
            } catch (InterruptedException ex) {
                System.out.println("Thread de inserção interrompida.");
            }
        });

        threadInsercao.start();
    }

    private void pararInsercao() {
        insercaoAtiva = false;
        System.out.println("Inserção encerrada!");
    }

    private void encerrarSimulacao() {
        insercaoAtiva = false;
        if (threadInsercao != null) {
            threadInsercao.interrupt();
        }

        for (Carro c : carrosAtivos) {
            c.interrupt(); // Força término das threads de carros
        }

        carrosAtivos.clear();

        for (Quadrante q : malhaController.getMalha().getQuadrantes().values()) {
            q.removerCarro();
        }
        Platform.runLater(malhaView::atualizarCelulas);

        System.out.println("Simulação encerrada!");
        Platform.runLater(() -> malhaView.exibirMensagemFinal(true));
    }

    private Carro criarNovoCarro(int entradaIndex) {
        Quadrante entrada = malhaController.getPontosDeEntrada().get(entradaIndex);
        Carro carro = new Carro(entrada, 800, malhaView);
        carro.setName("Carro-" + System.currentTimeMillis());

        entrada.adicionarCarro(carro);
        entrada.setQuadranteDoCarro();
        Platform.runLater(malhaView::atualizarCelulas);

        return carro;
    }
}
