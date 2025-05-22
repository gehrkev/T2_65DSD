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
                    List<Direcao> saidas = verSaidasPossiveis(proximo);
                    Direcao direcaoEscolhida = sortearSaida(saidas);
                    List<Quadrante> caminho = percorrerCruzamento(atual, direcaoEscolhida);

                    if (caminho.isEmpty()) {
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }

                    if (reservarCaminho(caminho)) {
                        atravessarCruzamento(caminho);
                        // não dá continue — deixa o loop seguir para próxima iteração
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

    private List<Direcao> verSaidasPossiveis(Quadrante cruzamento) {
        List<Direcao> saidas = new ArrayList<>();
        for (Direcao d : cruzamento.getDirecoesPossiveis()) {
            Quadrante vizinho = cruzamento.getVizinho(d);
            if (vizinho == null) continue;

            if (!isCruzamento(vizinho.getDirecao())) {
                saidas.add(d);
            } else {
                // verifica se a partir do cruzamento há caminho válido
                for (Direcao d2 : vizinho.getDirecoesPossiveis()) {
                    Quadrante pos = vizinho.getVizinho(d2);
                    if (pos != null && !isCruzamento(pos.getDirecao())) {
                        saidas.add(d);
                        break;
                    }
                }
            }
        }
        return saidas;
    }


    private Direcao sortearSaida(List<Direcao> saidas) {
        if (saidas.isEmpty()) return null;
        return saidas.get(rand.nextInt(saidas.size()));
    }

    private List<Quadrante> percorrerCruzamento(Quadrante entrada, Direcao direcaoSaida) {
        List<Quadrante> caminho = new ArrayList<>();
        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();

        Quadrante inicio = entrada.getVizinho(entrada.getDirecao());
        if (inicio == null) return caminho;

        fila.add(inicio);
        Map<Quadrante, Quadrante> veioDe = new HashMap<>();

        Quadrante destino = null;

        while (!fila.isEmpty()) {
            Quadrante atual = fila.poll();
            visitados.add(atual);

            for (Direcao d : atual.getDirecoesPossiveis()) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho == null || visitados.contains(vizinho)) continue;

                veioDe.put(vizinho, atual);

                if (d == direcaoSaida && !isCruzamento(vizinho.getDirecao())) {
                    destino = vizinho;
                    veioDe.put(destino, atual);
                    fila.clear();
                    break;
                }

                if (isCruzamento(vizinho.getDirecao())) {
                    fila.add(vizinho);
                }
            }
        }

        if (destino == null) return caminho;

        Quadrante atual = destino;
        while (atual != null && atual != entrada) {
            caminho.add(0, atual);
            atual = veioDe.get(atual);
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
                caminho.get(i - 1).getSemaforo().release(); // libera o anterior
            }
        }

        // Libera o último semáforo após sair do cruzamento
        caminho.get(caminho.size() - 1).getSemaforo().release();
    }



    public long getVelocidade() {
        return velocidade;
    }

    public void setOnTermino(Runnable onTermino) {
        this.onTermino = onTermino;
    }
}
