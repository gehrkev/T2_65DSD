package br.udesc.dsd.model;

import java.util.HashMap;
import java.util.Map;

public class MalhaViaria {
    private final Map<String, Quadrante> quadrantes;
    private final int linhas;
    private final int colunas;

    public MalhaViaria(int linhas, int colunas) {
        this.linhas = linhas;
        this.colunas = colunas;
        this.quadrantes = new HashMap<>();

        criarQuadrantes(linhas, colunas);
    }

    private void criarQuadrantes(int linhas, int colunas) {
        for (int linha = 0; linha < linhas; linha++) {
            for (int coluna = 0; coluna < colunas; coluna++) {
                Quadrante quadrante = new Quadrante(linha, coluna, Direcao.NADA);
                quadrantes.put(linha + "," + coluna, quadrante);
            }
        }
    }

    public Quadrante getQuadrante(int linha, int coluna) {
        return quadrantes.get(linha + "," + coluna);
    }

    public Map<String, Quadrante> getQuadrantes() {
        return quadrantes;
    }

    public int getLinhas() {
        return linhas;
    }

    public int getColunas() {
        return colunas;
    }
}
