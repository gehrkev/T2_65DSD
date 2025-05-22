package br.udesc.dsd.model;

import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Carro extends Thread {
    private Quadrante quadranteAtual;
    private long velocidade; // thread sleep para movimentação entre quadrantes
    private final MalhaView malhaView;
    private Runnable onTermino;
    private boolean ativo = true;
    private final Random rand = new Random();

    public Carro(Quadrante quadranteInicial, long velocidade, MalhaView malhaView) {
        this.quadranteAtual = quadranteInicial;
        this.velocidade = velocidade;
        this.malhaView = malhaView;
        this.setName("Carro-" + System.currentTimeMillis());
    }

    public Quadrante getQuadranteAtual() { return quadranteAtual; }

    public void setQuadranteAtual(Quadrante quadrante) {
        this.quadranteAtual = quadrante;
    }

    @Override
    public void run() {
        try {
            Platform.runLater(malhaView::atualizarCelulas);

            while (ativo) {
                Quadrante atual = this.quadranteAtual;
                Direcao direcao = atual.getDirecao();

                Platform.runLater(malhaView::atualizarCelulas);

                Quadrante proximo = atual.getVizinho(direcao);
                if (proximo == null) {
                    System.out.println(getName() + " saiu da malha. Encerrando thread.");
                    atual.removerCarro();
                    Platform.runLater(malhaView::atualizarCelulas);
                    ativo = false;
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
                    boolean moveu = false;
                    do {
                        boolean adquiriuProximo = proximo.getSemaforo().tryAcquire(500, TimeUnit.MILLISECONDS);

                        if (adquiriuProximo) {
                            atual.removerCarro();
                            this.setQuadranteAtual(proximo);
                            proximo.adicionarCarro(this);

                            Platform.runLater(malhaView::atualizarCelulas);
                            System.out.println(getName() + " movido para: " + proximo);
                            moveu = true;
                        } else {
                            System.out.println(getName() + " não conseguiu mover para: " + proximo + ". Tentando novamente.");
                            Thread.sleep(rand.nextInt(500));
                        }
                    } while (!moveu && ativo);
                }

                Thread.sleep(velocidade);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(getName() + " interrompido.");
        }

        if (onTermino != null) onTermino.run();
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

    private boolean reservarCaminho(List<Quadrante> caminho) throws InterruptedException {
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
    }

    private void atravessarCruzamento(List<Quadrante> caminho) throws InterruptedException {
        for (int i = 0; i < caminho.size(); i++) {
            Quadrante proximo = caminho.get(i);
            Quadrante anterior = quadranteAtual;

            anterior.removerCarro();
            setQuadranteAtual(proximo);
            proximo.adicionarCarro(this);

            Platform.runLater(malhaView::atualizarCelulas);
            System.out.println(getName() + ": Moveu para " + proximo + " no cruzamento.");

            Thread.sleep(velocidade / 2);

            if (i > 0) {
                caminho.get(i - 1).getSemaforo().release();
            }
        }

        if (!caminho.isEmpty()) {
            caminho.get(caminho.size() - 1).getSemaforo().release();
        }
    }

    public long getVelocidade() {
        return velocidade;
    }

    public void setOnTermino(Runnable onTermino) {
        this.onTermino = onTermino;
    }
}