package br.udesc.dsd.model;

import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

        // Se o vizinho da frente estiver ocupado por outro carro, ele espera.q (Thread.wait())
        // Caso contrário, move-se para aquele quadrante. Não esquecer de desocupar o quadrante antigo depois de mover-se
        // (quadranteAtual.carro = null)
        // () -> quadranteAtual.getVizinhosDaFrente().forEach() ??

        // Adicionar casos especiais para CRUZAMENTO_CIMA(5), CRUZAMENTO_DIREITA(6), CRUZAMENTO_BAIXO(7),
        //    CRUZAMENTO_ESQUERDA(8), CRUZAMENTO_CIMA_E_DIREITA(9), CRUZAMENTO_CIMA_E_ESQUERDA(10),
        //    CRUZAMENTO_BAIXO_E_DIREITA(11), CRUZAMENTO_BAIXO_E_ESQUERDA(12):
        //
        // Se o quadrante atual NÃO FOR cruzamento E o vizinho da frente for CRUZAMENTO,
        // A malha deve retornar o set dos 4 quadrantes que compõe o cruzamento, e outro set com os possíveis destinos
        // O carro deve definir seu destino aleatoriamente antes de iniciar
        //
        // Ele somente deverá iniciar o percurso do cruzamento ao garantir que todos os quadrantes do cruzamento
        // e o destino estão livres. Ao sair ele deve sinalizar ao semaforo.
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
                    List<Quadrante> caminhoCruzamento = determinarCaminhoCruzamento(atual, proximo);

                    if (caminhoCruzamento.isEmpty()) {
                        System.out.println(getName() + ": Não encontrou caminho válido pelo cruzamento. Aguardando.");
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }

                    boolean atravessou = atravessarCruzamento(atual, caminhoCruzamento);
                    if (!atravessou) {
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
                            Thread.sleep(rand.nextInt(500)); // Aguarda tempo aleatório entre 0-499ms
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

    // TODO Checar lógica, os carros sempre estão saindo pela mesmo caminho
    /**
     * Determina o caminho a ser percorrido através do cruzamento.
     * @return Lista de quadrantes formando o caminho pelo cruzamento, incluindo a saída
     */
    private List<Quadrante> determinarCaminhoCruzamento(Quadrante quadranteInicial, Quadrante primeiroCruzamento) {
        List<Quadrante> caminho = new ArrayList<>();

        caminho.add(primeiroCruzamento);

        Quadrante atual = primeiroCruzamento;
        Set<String> visitados = new HashSet<>();
        String chaveAtual = atual.getLinha() + "," + atual.getColuna();
        visitados.add(chaveAtual);

        Direcao direcaoEntrada = quadranteInicial.getDirecao();

        boolean encontrouSaida = false;
        int passos = 0;
        final int MAX_PASSOS = 5; // Limite para evitar loops infinitos

        while (!encontrouSaida && passos < MAX_PASSOS) {
            passos++;

            Set<Direcao> direcoesPossiveis = atual.getDirecoesPossiveis();

            List<Direcao> direcoes = new ArrayList<>(direcoesPossiveis);

            if (atual == primeiroCruzamento) {
                direcoes.sort((d1, d2) -> {
                    if (d1 == direcaoEntrada) return -1;
                    if (d2 == direcaoEntrada) return 1;
                    return 0;
                });
            }

            boolean moveu = false;

            for (Direcao d : direcoes) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho != null) {
                    String chaveVizinho = vizinho.getLinha() + "," + vizinho.getColuna();

                    if (!visitados.contains(chaveVizinho)) {
                        visitados.add(chaveVizinho);

                        if (isCruzamento(vizinho.getDirecao())) {
                            caminho.add(vizinho);
                            atual = vizinho;
                            moveu = true;
                            break;
                        } else {
                            caminho.add(vizinho);
                            encontrouSaida = true;
                            moveu = true;
                            break;
                        }
                    }
                }
            }

            if (!moveu && !encontrouSaida) {
                System.out.println(getName() + ": Não foi possível encontrar uma saída válida do cruzamento.");
                return new ArrayList<>();
            }
        }

        if (!encontrouSaida) {
            System.out.println(getName() + ": Atingiu o limite de passos sem encontrar saída do cruzamento.");
            return new ArrayList<>();
        }

        return caminho;
    }

    private boolean atravessarCruzamento(Quadrante quadranteAtual, List<Quadrante> caminho) throws InterruptedException {
        List<Quadrante> semafAdquiridos = new ArrayList<>();

        try {
            for (Quadrante q : caminho) {
                boolean adquirido = q.getSemaforo().tryAcquire(500, TimeUnit.MILLISECONDS);
                if (!adquirido) {
                    for (Quadrante qa : semafAdquiridos) {
                        qa.getSemaforo().release();
                    }
                    System.out.println(getName() + ": Não conseguiu adquirir todos os semáforos do cruzamento.");
                    return false;
                }
                semafAdquiridos.add(q);
            }

            System.out.println(getName() + ": Adquiriu todos os semáforos para atravessar o cruzamento.");

            Quadrante atual = quadranteAtual;

            for (int i = 0; i < caminho.size(); i++) {
                Quadrante proximo = caminho.get(i);

                atual.removerCarro();

                this.setQuadranteAtual(proximo);
                proximo.adicionarCarro(this);

                Platform.runLater(malhaView::atualizarCelulas);
                System.out.println(getName() + ": Moveu para " + proximo + " dentro do cruzamento.");

                atual = proximo;

                Thread.sleep(velocidade / 2);
            }

            return true;
        } catch (InterruptedException e) {
            for (Quadrante q : semafAdquiridos) {
                if (q != this.quadranteAtual) {
                    q.getSemaforo().release();
                }
            }
            throw e;
        }
    }

    public long getVelocidade() {
        return velocidade;
    }

    public void setOnTermino(Runnable onTermino) {
        this.onTermino = onTermino;
    }
}