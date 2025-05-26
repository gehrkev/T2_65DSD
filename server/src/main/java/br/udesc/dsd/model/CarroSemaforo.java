package br.udesc.dsd.model;

import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javafx.scene.paint.Color;

public class CarroSemaforo extends Thread implements ICarro {
    private Quadrante quadranteAtual;
    private final long velocidade; // thread sleep para movimentação entre quadrantes
    private final MalhaView malhaView;
    private final Color cor;
    private static final Color[] CORES_DISPONIVEIS = {
            Color.ORANGERED, Color.STEELBLUE, Color.FORESTGREEN, Color.DARKORANGE,
            Color.MEDIUMPURPLE, Color.GOLDENROD, Color.DEEPPINK, Color.LIGHTSKYBLUE
    };

    private volatile boolean segurandoSemaforo = false;
    private volatile boolean ativo = true;
    private volatile boolean shutdownRequested = false;
    private Runnable onTermino;
    private final Random rand = new Random();

    public CarroSemaforo(Quadrante quadranteInicial, long velocidade, MalhaView malhaView) {
        this.quadranteAtual = quadranteInicial;
        this.velocidade = velocidade;
        this.malhaView = malhaView;
        this.cor = CORES_DISPONIVEIS[rand.nextInt(CORES_DISPONIVEIS.length)];
        this.setName("Carro-" + System.currentTimeMillis() + "-" + rand.nextInt(1000));
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);

            segurandoSemaforo = true;

            while (ativo && !shutdownRequested) {
                Quadrante atual = this.quadranteAtual;
                Direcao direcao = atual.getDirecao();

                Quadrante proximo = atual.getVizinho(direcao);
                if (proximo == null) {
                    System.out.println(getName() + " saiu da malha. Encerrando thread.");
                    break;
                }

                boolean entrandoEmCruzamento = !isCruzamento(atual.getDirecao()) && isCruzamento(proximo.getDirecao());

                if (entrandoEmCruzamento) {
                    List<Quadrante> saidasPossiveis = coletarSaidasPossiveis(atual);

                    if (saidasPossiveis.isEmpty()) {
                        System.out.println(getName() + " não encontrou saídas do cruzamento. Tentando novamente.");
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }

                    Quadrante saidaEscolhida = saidasPossiveis.get(rand.nextInt(saidasPossiveis.size()));
                    System.out.println(getName() + " escolheu saída: " + saidaEscolhida);

                    List<Quadrante> caminho = encontrarCaminhoParaSaida(atual, saidaEscolhida);

                    if (caminho.isEmpty()) {
                        System.out.println(getName() + " não conseguiu encontrar caminho para a saída escolhida.");
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }

                    if (reservarCaminho(caminho)) {
                        atravessarCruzamento(caminho);
                    } else {
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }
                } else {
                    if (moverParaQuadrante(proximo)) {
                        System.out.println(getName() + " movido para: " + proximo);
                    } else {
                        System.out.println(getName() + " não conseguiu mover para: " + proximo + ". Tentando novamente.");
                        Thread.sleep(rand.nextInt(500));
                    }
                }

                Thread.sleep(velocidade);
            }
        } catch (InterruptedException e) {
            if (!shutdownRequested) {
                System.out.println(getName() + " interrompido inesperadamente.");
            }
        } finally {
            cleanup();
        }

        if (onTermino != null) onTermino.run();
    }

    private boolean moverParaQuadrante(Quadrante proximo) {
        try {
            boolean acquired = proximo.getSemaforo().tryAcquire(500, TimeUnit.MILLISECONDS);

            if (acquired) {
                Quadrante atual = this.quadranteAtual;

                atual.setCarro(null);
                this.quadranteAtual = proximo;
                proximo.setCarro(this);

                if (segurandoSemaforo) {
                    atual.getSemaforo().release();
                }

                segurandoSemaforo = true;

                Platform.runLater(() -> malhaView.atualizarQuadrantes(atual, proximo));
                return true;
            }

            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isCruzamento(Direcao direcao) {
        return direcao == Direcao.CRUZAMENTO_CIMA ||
                direcao == Direcao.CRUZAMENTO_BAIXO ||
                direcao == Direcao.CRUZAMENTO_ESQUERDA ||
                direcao == Direcao.CRUZAMENTO_DIREITA ||
                direcao == Direcao.CRUZAMENTO_CIMA_E_DIREITA ||
                direcao == Direcao.CRUZAMENTO_CIMA_E_ESQUERDA ||
                direcao == Direcao.CRUZAMENTO_BAIXO_E_DIREITA ||
                direcao == Direcao.CRUZAMENTO_BAIXO_E_ESQUERDA;
    }

    /** BFS para coletar todas as saídas possíveis (vizinhos não-cruzamento) */
    private List<Quadrante> coletarSaidasPossiveis(Quadrante entrada) {
        List<Quadrante> saidas = new ArrayList<>();
        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();

        Quadrante inicio = entrada.getVizinho(entrada.getDirecao());
        if (inicio == null) return saidas;

        fila.add(inicio);
        visitados.add(inicio);

        while (!fila.isEmpty()) {
            Quadrante atual = fila.poll();

            for (Direcao d : atual.getDirecoesPossiveis()) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho == null || visitados.contains(vizinho)) continue;

                visitados.add(vizinho);

                if (isCruzamento(vizinho.getDirecao())) {
                    fila.add(vizinho);
                } else {
                    saidas.add(vizinho);
                }
            }
        }

        return saidas;
    }

    /** BFS para encontrar o caminho específico da entrada até a saída escolhida */
    private List<Quadrante> encontrarCaminhoParaSaida(Quadrante entrada, Quadrante saidaAlvo) {
        List<Quadrante> caminho = new ArrayList<>();
        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();
        Map<Quadrante, Quadrante> veioDe = new HashMap<>();

        Quadrante inicio = entrada.getVizinho(entrada.getDirecao());
        if (inicio == null) return caminho;

        fila.add(inicio);
        visitados.add(inicio);
        veioDe.put(inicio, null);

        boolean encontrouSaida = false;

        while (!fila.isEmpty() && !encontrouSaida) {
            Quadrante atual = fila.poll();

            for (Direcao d : atual.getDirecoesPossiveis()) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho == null || visitados.contains(vizinho)) continue;

                visitados.add(vizinho);
                veioDe.put(vizinho, atual);

                if (vizinho == saidaAlvo) {
                    encontrouSaida = true;
                    break;
                }

                if (isCruzamento(vizinho.getDirecao())) {
                    fila.add(vizinho);
                }
            }
        }

        if (!encontrouSaida) return caminho;

        Quadrante atual = saidaAlvo;
        while (atual != null && veioDe.get(atual) != null) {
            caminho.add(0, atual);
            atual = veioDe.get(atual);
        }

        if (atual != null) {
            caminho.add(0, atual);
        }

        return caminho;
    }

    private boolean reservarCaminho(List<Quadrante> caminho) {
        try {
            List<Quadrante> adquiridos = new ArrayList<>();

            for (Quadrante q : caminho) {
                boolean acquired = q.getSemaforo().tryAcquire(500, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    for (Quadrante aq : adquiridos) {
                        aq.getSemaforo().release();
                    }
                    return false;
                }
                adquiridos.add(q);
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void atravessarCruzamento(List<Quadrante> caminho) {
        try {
            for (int i = 0; i < caminho.size(); i++) {
                Quadrante proximo = caminho.get(i);
                Quadrante atual = this.quadranteAtual;

                atual.setCarro(null);
                this.quadranteAtual = proximo;
                proximo.setCarro(this);

                if (i == 0 && segurandoSemaforo) {
                    atual.getSemaforo().release();
                } else if (i > 0) {
                    caminho.get(i - 1).getSemaforo().release();
                }

                Platform.runLater(() -> malhaView.atualizarQuadrantes(atual, proximo));
                System.out.println(getName() + ": Moveu para " + proximo + " no cruzamento.");

                Thread.sleep(velocidade);
            }
            segurandoSemaforo = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void cleanup() {
        if (quadranteAtual != null) {
            Quadrante ultimo = quadranteAtual;
            quadranteAtual.setCarro(null);
            if (segurandoSemaforo) {
                quadranteAtual.getSemaforo().release();
                segurandoSemaforo = false;
            }
            Platform.runLater(() -> malhaView.atualizarQuadrante(ultimo));
        }
        System.out.println(getName() + " finalizou corretamente.");
    }

    public void requestShutdown() {
        this.shutdownRequested = true;
        this.ativo = false;
        this.interrupt();
    }

    public Color getCor() {
        return cor;
    }

    public void setQuadranteAtual(Quadrante quadrante) {
        this.quadranteAtual = quadrante;
    }

    public void setOnTermino(Runnable onTermino) {
        this.onTermino = onTermino;
    }
}