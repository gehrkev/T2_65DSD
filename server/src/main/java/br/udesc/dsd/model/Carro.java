package br.udesc.dsd.model;

public class Carro extends Thread {
    private Quadrante quadranteAtual;
    private long velocidade; // thread sleep para movimentação entre quadrantes

    public Carro(Quadrante quadranteInicial) {
        this.quadranteAtual = quadranteInicial;
    }

    public Quadrante getQuadranteAtual() { return quadranteAtual; }

    public void setQuadranteAtual(Quadrante quadrante) {
        this.quadranteAtual = quadrante;
    }

    @Override
    public void run() {
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

    }
}

