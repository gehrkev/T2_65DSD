package br.udesc.dsd.model;

import javafx.scene.paint.Color;

public interface ICarro extends Runnable {
    void start();
    void requestShutdown();
    void setOnTermino(Runnable onTermino);

    void setQuadranteAtual(Quadrante quadrante);
    Color getCor();
    String getName();
    boolean isAlive();
    void join(long millis) throws InterruptedException;
    void interrupt();
}