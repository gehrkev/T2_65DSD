package br.udesc.dsd.model;

import java.util.Objects;

public class Carro extends Thread {
    private Direcao direcaoAtual;
    private long velocidade; // thread sleep para movimentação entre quadrantes

    public Carro(Direcao direcaoInicial) {
        this.direcaoAtual = direcaoInicial;
    }

    public Direcao getDirecaoAtual() { return direcaoAtual; }

    public void setDirecaoAtual(Direcao direcao) {
        this.direcaoAtual = direcao;
    }

    @Override
    public void run() {

    }
}
