package br.udesc.dsd.controller;

import br.udesc.dsd.model.CarroMonitor;
import br.udesc.dsd.model.Quadrante;
import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimulacaoControllerMonitor implements ISimulacaoController {

    private final MalhaController malhaController;
    private final MalhaView malhaView;

    private Thread threadInsercao;
    private final List<CarroMonitor> carrosAtivos = new ArrayList<>();
    private volatile boolean insercaoAtiva = false;

    public SimulacaoControllerMonitor(MalhaController malhaController, MalhaView malhaView) {
        this.malhaController = malhaController;
        this.malhaView = malhaView;
    }

    private synchronized int getNumeroCarrosAtivos() {
        return carrosAtivos.size();
    }

    private synchronized boolean adicionarCarro(CarroMonitor carro) {
        carrosAtivos.add(carro);
        System.out.println("Carro adicionado: " + carro.getName() + ". Total ativo: " + carrosAtivos.size());
        return true;
    }

    private synchronized boolean removerCarro(CarroMonitor carro) {
        carrosAtivos.remove(carro);
        System.out.println("Carro removido: " + carro.getName() + ". Total ativo: " + carrosAtivos.size());
        return true;
    }

    private synchronized List<CarroMonitor> getCarrosAtivos() {
        return new ArrayList<>(carrosAtivos);
    }

    private synchronized boolean carrosAtivosVazio() {
        return carrosAtivos.isEmpty();
    }

    private synchronized void limparCarrosAtivos() {
        carrosAtivos.clear();
    }

    public void iniciarSimulacao() {
        Platform.runLater(() -> malhaView.exibirMensagemFinal(false));
        if (insercaoAtiva) return;

        int limite = malhaView.limiteVeiculosSpinner.getValue();
        int intervalo = malhaView.intervaloSpinner.getValue();

        insercaoAtiva = true;

        threadInsercao = new Thread(() -> {
            while (insercaoAtiva) {
                try {
                    if (getNumeroCarrosAtivos() < limite) {
                        Platform.runLater(() -> {
                            CarroMonitor carro = criarNovoCarro();
                            if (carro != null) {
                                if (adicionarCarro(carro)) {
                                    carro.setOnTermino(() -> removerCarro(carro));
                                    carro.start();
                                }
                            }
                        });
                    }
                    Thread.sleep(intervalo);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            System.out.println("Thread de inserção encerrada.");
        });

        threadInsercao.setName("Thread-Insercao");
        threadInsercao.start();
    }

    public void pararInsercao() {
        insercaoAtiva = false;
        System.out.println("Inserção encerrada!");
    }

    public void encerrarSimulacao() {
        insercaoAtiva = false;

        if (threadInsercao != null && threadInsercao.isAlive()) {
            threadInsercao.interrupt();
            try {
                threadInsercao.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        List<CarroMonitor> carrosParaFinalizar = getCarrosAtivos();
        System.out.println("Solicitando parada de " + carrosParaFinalizar.size() + " carros...");

        for (CarroMonitor carro : carrosParaFinalizar) {
            carro.requestShutdown();
        }

        long shutdownStart = System.currentTimeMillis();
        long timeout = 5000;

        while (!carrosAtivosVazio() && (System.currentTimeMillis() - shutdownStart) < timeout) {
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
            System.out.println("Forçando parada dos carros restantes...");
            for (CarroMonitor carro : getCarrosAtivos()) {
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

        for (Quadrante q : malhaController.getMalha().getQuadrantes().values()) {
            synchronized (q) {
                q.setCarro(null);
                q.notifyAll();
            }
        }

        Platform.runLater(() -> {
            malhaView.atualizarCelulas();
            malhaView.exibirMensagemFinal(true);
        });

        System.out.println("Simulação encerrada!");
    }

    private CarroMonitor criarNovoCarro() {
        Random rand = new Random();
        List<Quadrante> entradas = malhaController.getPontosDeEntrada();

        for (int tentativa = 0; tentativa < 10; tentativa++) {
            int entradaIndex = rand.nextInt(entradas.size());
            Quadrante entrada = entradas.get(entradaIndex);

            synchronized (entrada) {
                if (entrada.getCarro() != null) continue;

                long velocidadeAleatoria = 500 + rand.nextInt(601);
                String nomeUnico = String.format("Carro-%d-%d",
                        System.currentTimeMillis(),
                        rand.nextInt(1000));

                CarroMonitor carro = new CarroMonitor(entrada, velocidadeAleatoria, malhaView);
                carro.setName(nomeUnico);

                entrada.setCarro(carro);
                entrada.setQuadranteDoCarro();

                Platform.runLater(() -> malhaView.atualizarQuadrante(entrada));

                return carro;
            }
        }

        System.out.println("Não foi possível criar novo carro após várias tentativas.");
        return null;
    }
}
