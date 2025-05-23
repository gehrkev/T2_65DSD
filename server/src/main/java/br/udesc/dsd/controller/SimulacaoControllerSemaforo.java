package br.udesc.dsd.controller;

import br.udesc.dsd.model.CarroSemaforo;
import br.udesc.dsd.model.Quadrante;
import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SimulacaoControllerSemaforo implements ISimulacaoController {

    private final MalhaController malhaController;
    private final MalhaView malhaView;

    private Thread threadInsercao;
    private final List<CarroSemaforo> carrosAtivos = new ArrayList<>();
    private final Semaphore semaforoListaCarros = new Semaphore(1, true);
    private volatile boolean insercaoAtiva = false;

    public SimulacaoControllerSemaforo(MalhaController malhaController, MalhaView malhaView) {
        this.malhaController = malhaController;
        this.malhaView = malhaView;
    }

    private int getNumeroCarrosAtivos() {
        try {
            semaforoListaCarros.acquire();
            try {
                return carrosAtivos.size();
            } finally {
                semaforoListaCarros.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    private boolean adicionarCarro(CarroSemaforo carro) {
        try {
            semaforoListaCarros.acquire();
            try {
                carrosAtivos.add(carro);
                System.out.println("Carro adicionado: " + carro.getName() +
                        ". Total ativo: " + carrosAtivos.size());
                return true;
            } finally {
                semaforoListaCarros.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean removerCarro(CarroSemaforo carro) {
        try {
            semaforoListaCarros.acquire();
            try {
                carrosAtivos.remove(carro);
                System.out.println("Carro removido: " + carro.getName() +
                        ". Total ativo: " + carrosAtivos.size());
                return true;
            } finally {
                semaforoListaCarros.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private List<CarroSemaforo> getCarrosAtivos() {
        try {
            semaforoListaCarros.acquire();
            try {
                return new ArrayList<>(carrosAtivos);
            } finally {
                semaforoListaCarros.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    private boolean carrosAtivosVazio() {
        try {
            semaforoListaCarros.acquire();
            try {
                return carrosAtivos.isEmpty();
            } finally {
                semaforoListaCarros.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void limparCarrosAtivos() {
        try {
            semaforoListaCarros.acquire();
            try {
                carrosAtivos.clear();
            } finally {
                semaforoListaCarros.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                            CarroSemaforo carro = criarNovoCarro();
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

        List<CarroSemaforo> carrosParaFinalizar = getCarrosAtivos();
        System.out.println("Solicitando parada de " + carrosParaFinalizar.size() + " carros...");

        for (CarroSemaforo carro : carrosParaFinalizar) {
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
            for (CarroSemaforo carro : getCarrosAtivos()) {
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
            q.setCarro(null);
        }

        Platform.runLater(() -> {
            malhaView.atualizarCelulas();
            malhaView.exibirMensagemFinal(true);
        });

        System.out.println("Simulação encerrada!");
    }

    private CarroSemaforo criarNovoCarro() {
        Random rand = new Random();
        List<Quadrante> entradas = malhaController.getPontosDeEntrada();

        for (int tentativa = 0; tentativa < 10; tentativa++) {
            int entradaIndex = rand.nextInt(entradas.size());
            Quadrante entrada = entradas.get(entradaIndex);

            if (entrada.getCarro() != null) {
                continue;
            }

            try {
                if (entrada.getSemaforo().tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    long velocidadeAleatoria = 400 + rand.nextInt(801);
                    String nomeUnico = String.format("Carro-%d-%d",
                            System.currentTimeMillis(),
                            rand.nextInt(1000));

                    CarroSemaforo carro = new CarroSemaforo(entrada, velocidadeAleatoria, malhaView);
                    carro.setName(nomeUnico);

                    entrada.setCarro(carro);
                    entrada.setQuadranteDoCarro();

                    Platform.runLater(() -> malhaView.atualizarQuadrante(entrada));
                    return carro;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        System.out.println("Não foi possível criar novo carro após várias tentativas.");
        return null;
    }
}