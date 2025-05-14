package br.udesc.dsd.model;

public enum Direcao {
    NADA(0), CIMA(1), DIREITA(2), BAIXO(3),
    ESQUERDA(4), CRUZAMENTO_CIMA(5), CRUZAMENTO_DIREITA(6), CRUZAMENTO_BAIXO(7),
    CRUZAMENTO_ESQUERDA(8), CRUZAMENTO_CIMA_E_DIREITA(9), CRUZAMENTO_CIMA_E_ESQUERDA(10),
    CRUZAMENTO_BAIXO_E_DIREITA(11), CRUZAMENTO_BAIXO_E_ESQUERDA(12);

    private final int valor;

    Direcao(int valor) {
        this.valor = valor;
    }

    public int getValor() { return valor; }

    public static Direcao fromValor(int valor) {
        for (Direcao d : values()) {
            if (d.valor == valor) return d;
        }
        return null;
    }
}
