package br.udesc.dsd.controller;

import br.udesc.dsd.model.Carro;
import br.udesc.dsd.model.Quadrante;
import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.Semaphore;

public class SimulacaoController {

    private final MalhaController malhaController;
    private final MalhaView malhaView;

    private Thread threadInsercao;
    private final List<Carro> carrosAtivos = new ArrayList<>();
    private final Semaphore semaforoListaCarros = new Semaphore(1, true); // evita problemas de insert/remove com a threadInsercao
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

    private int getNumeroCarrosAtivos() throws InterruptedException {
        semaforoListaCarros.acquire();
        try {
            return carrosAtivos.size();
        } finally {
            semaforoListaCarros.release();
        }
    }

    private void adicionarCarro(Carro carro) throws InterruptedException {
        semaforoListaCarros.acquire();
        try {
            carrosAtivos.add(carro);
            System.out.println("Carro adicionado: " + carro.getName() +
                    ". Total ativo: " + carrosAtivos.size());
        } finally {
            semaforoListaCarros.release();
        }
    }

    private void removerCarro(Carro carro) throws InterruptedException {
        semaforoListaCarros.acquire();
        try {
            carrosAtivos.remove(carro);
            System.out.println("Carro removido: " + carro.getName() +
                    ". Total ativo: " + carrosAtivos.size());
        } finally {
            semaforoListaCarros.release();
        }
    }

    private List<Carro> getCarrosAtivos() throws InterruptedException {
        semaforoListaCarros.acquire();
        try {
            return new ArrayList<>(carrosAtivos);
        } finally {
            semaforoListaCarros.release();
        }
    }

    private boolean carrosAtivosVazio() throws InterruptedException {
        semaforoListaCarros.acquire();
        try {
            return carrosAtivos.isEmpty();
        } finally {
            semaforoListaCarros.release();
        }
    }

    private void limparCarrosAtivos() throws InterruptedException {
        semaforoListaCarros.acquire();
        try {
            carrosAtivos.clear();
        } finally {
            semaforoListaCarros.release();
        }
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
                    try {
                        if (getNumeroCarrosAtivos() < limite) {
                            Platform.runLater(() -> {
                                try {
                                    int entradaIndex = (int) (Math.random() *
                                            malhaController.getPontosDeEntrada().size());
                                    Carro carro = criarNovoCarro(entradaIndex);

                                    if (carro != null) {
                                        adicionarCarro(carro);

                                        carro.setOnTermino(() -> {
                                            try {
                                                removerCarro(carro);
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                                System.err.println("Erro ao remover carro: " + e.getMessage());
                                            }
                                        });

                                        carro.start();
                                    }
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    System.err.println("Erro ao criar carro: " + e.getMessage());
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    Thread.sleep(intervalo);
                }
            } catch (InterruptedException ex) {
                System.out.println("Thread de inserção interrompida.");
            }
        });

        threadInsercao.setName("Thread-Insercao");
        threadInsercao.start();
    }

    private void pararInsercao() {
        insercaoAtiva = false;
        System.out.println("Inserção encerrada!");
    }

    private void encerrarSimulacao() {
        insercaoAtiva = false;

        try {
            if (threadInsercao != null && threadInsercao.isAlive()) {
                threadInsercao.interrupt();
                try {
                    threadInsercao.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            List<Carro> carrosParaFinalizar = getCarrosAtivos();
            System.out.println("Solicitando parada de " + carrosParaFinalizar.size() + " carros...");

            for (Carro carro : carrosParaFinalizar) {
                carro.requestShutdown();
            }

            long shutdownStart = System.currentTimeMillis();
            long timeout = 5000;

            while (!carrosAtivosVazio() &&
                    (System.currentTimeMillis() - shutdownStart) < timeout) {
                try {
                    int remaining = getNumeroCarrosAtivos();
                    System.out.println("Aguardando " + remaining + " carros finalizarem...");

                    Thread.sleep(100);
                    Platform.runLater(malhaView::atualizarCelulas);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!carrosAtivosVazio()) {
                System.out.println("Aguardando threads restantes com join...");
                List<Carro> carrosRestantes = getCarrosAtivos();

                for (Carro carro : carrosRestantes) {
                    try {
                        carro.join(500);
                        if (carro.isAlive()) {
                            System.out.println(carro.getName() + " não finalizou no tempo esperado.");
                            carro.interrupt();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            limparCarrosAtivos();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Erro durante encerramento da simulação: " + e.getMessage());
        }

        for (Quadrante q : malhaController.getMalha().getQuadrantes().values()) {
            q.setCarro(null);
        }

        Platform.runLater(() -> {
            malhaView.atualizarCelulas();
            malhaView.exibirMensagemFinal(true);
        });

        System.out.println("Simulação encerrada!");
    }

    private Carro criarNovoCarro(int entradaIndex) {
        Quadrante entrada = malhaController.getPontosDeEntrada().get(entradaIndex);
        Random rand = new Random();
        long velocidadeAleatoria = 200 + rand.nextInt(801);

        String nomeUnico = String.format("Carro-%d-%d",
                System.currentTimeMillis(),
                rand.nextInt(1000));

        Carro carro = new Carro(entrada, velocidadeAleatoria, malhaView);
        carro.setName(nomeUnico);

        try {
            entrada.getSemaforo().acquire();
            entrada.setCarro(carro);
            entrada.setQuadranteDoCarro();

            Platform.runLater(() -> {
                malhaView.atualizarQuadrante(entrada);
            });

            return carro;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Não foi possível criar novo carro: " + e.getMessage());
            return null;
        }
    }
}