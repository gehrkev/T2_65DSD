package br.udesc.dsd.model;

import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;

import java.util.Random;
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
            // Atualiza a interface gráfica inicialmente
            Platform.runLater(malhaView::atualizarCelulas);

            while (ativo) {
                Quadrante atual = this.quadranteAtual;
                Direcao direcao = atual.getDirecao();

                // Atualiza a interface após cada movimento
                Platform.runLater(malhaView::atualizarCelulas);

                // Verifica se há vizinho na direção permitida
                Quadrante proximo = atual.getVizinho(direcao);
                if (proximo == null) {
                    System.out.println(getName() + " saiu da malha. Encerrando thread.");
                    atual.removerCarro();
                    Platform.runLater(malhaView::atualizarCelulas);
                    ativo = false;
                    break;
                }

                boolean moveu = false;
                do {
                    // Tenta adquirir o próximo quadrante com timeout
                    boolean adquiriuProximo = proximo.getSemaforo().tryAcquire(500, TimeUnit.MILLISECONDS);

                    if (adquiriuProximo) {
                        // Conseguiu o próximo quadrante, agora faz a movimentação
                        atual.removerCarro(); // Isso libera o semáforo do quadrante atual
                        this.setQuadranteAtual(proximo);
                        proximo.adicionarCarro(this);

                        // Atualiza a interface após o movimento
                        Platform.runLater(malhaView::atualizarCelulas);
                        System.out.println(getName() + " movido para: " + proximo);
                        moveu = true;
                    } else {
                        // Não conseguiu o próximo quadrante, aguarda tempo aleatório
                        System.out.println(getName() + " não conseguiu mover para: " + proximo + ". Tentando novamente.");
                        Thread.sleep(rand.nextInt(500)); // Aguarda tempo aleatório entre 0-499ms
                    }
                } while (!moveu && ativo);

                // Espera entre movimentos
                Thread.sleep(velocidade);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(getName() + " interrompido.");
        }

        // Executa a ação de término, se houver
        if (onTermino != null) onTermino.run();
    }

    public long getVelocidade() {
        return velocidade;
    }

    public void setOnTermino(Runnable onTermino) {
        this.onTermino = onTermino;
    }

}

