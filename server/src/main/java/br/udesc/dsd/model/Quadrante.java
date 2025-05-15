package br.udesc.dsd.model;

import java.util.*;

public class Quadrante {
    private final int linha;
    private final int coluna;
    private boolean entrada;
    private Direcao direcao;
    private Carro carro;
    private final Map<Direcao, Quadrante> vizinhosDaFrente;

    public Quadrante(int linha, int coluna, Direcao direcao) {
        this.linha = linha;
        this.coluna = coluna;
        this.entrada = false;
        this.direcao = direcao;
        this.carro = null;
        this.vizinhosDaFrente = new EnumMap<>(Direcao.class);
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    public Direcao getDirecao() {
        return direcao;
    }

    public void setDirecao(Direcao direcao) {
        this.direcao = direcao;
    }

    public boolean temCarro() {
        return carro != null;
    }

    public Carro getCarro() {
        return carro;
    }

    public void adicionarCarro(Carro carro) {
        if (!temCarro()) {
            this.carro = carro;
        }
    }

    public void removerCarro() {
        this.carro = null;
    }

    public void adicionarVizinho(Direcao direcao, Quadrante vizinho) {
        vizinhosDaFrente.put(direcao, vizinho);
    }

    public Quadrante getVizinho(Direcao direcao) {
        return vizinhosDaFrente.get(direcao);
    }

    public Map<Direcao, Quadrante> getVizinhosDaFrente() {
        return Collections.unmodifiableMap(vizinhosDaFrente);
    }

    public Set<Direcao> getDirecoesPossiveis() {
        return vizinhosDaFrente.keySet();
    }

    // TODO setar direcao do carro ao ele entrar no quadrante.
    public void setQuadranteDoCarro() {
        this.carro.setQuadranteAtual(this);
    }

    public boolean isEntrada() {
        return entrada;
    }

    public void setEntrada(boolean entrada) {
        this.entrada = entrada;
    }

    @Override
    public String toString() {
        return "Quadrante{" +
                "linha=" + linha +
                ", coluna=" + coluna +
                ", direcao=" + direcao +
                ", temCarro=" + temCarro() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quadrante quadrante = (Quadrante) o;
        return linha == quadrante.linha &&
                coluna == quadrante.coluna;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linha, coluna);
    }
}