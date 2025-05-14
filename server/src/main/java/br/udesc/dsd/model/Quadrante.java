package br.udesc.dsd.model;

import java.util.*;

public class Quadrante {
    private final int linha;
    private final int coluna;
    private Direcao direcao;
    private Carro carro;
    private Map<Direcao, Quadrante> vizinhos;

    public Quadrante(int linha, int coluna, Direcao direcao) {
        this.linha = linha;
        this.coluna = coluna;
        this.direcao = direcao;
        this.carro = null;
        this.vizinhos = new EnumMap<>(Direcao.class);
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
        vizinhos.put(direcao, vizinho);
    }

    public Quadrante getVizinho(Direcao direcao) {
        return vizinhos.get(direcao);
    }

    public Map<Direcao, Quadrante> getVizinhos() {
        return Collections.unmodifiableMap(vizinhos);
    }

    public Set<Direcao> getDirecoesPossiveis() {
        return vizinhos.keySet();
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