package br.udesc.dsd.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MalhaViaria {
    private Map<String, Quadrante> quadrantes;
    private int linhas;
    private int colunas;

    private MalhaViaria(int linhas, int colunas) {
        this.linhas = linhas;
        this.colunas = colunas;
        this.quadrantes = new HashMap<>();

        for (int linha = 0; linha < linhas; linha++) {
            for (int coluna = 0; coluna < colunas; coluna++) {
                Quadrante quadrante = new Quadrante(linha, coluna, Direcao.NADA);
                quadrantes.put(criarChave(linha, coluna), quadrante);
            }
        }
    }

    private String criarChave(int linha, int coluna) {
        return linha + "," + coluna;
    }

    public static MalhaViaria carregarDeArquivo(String caminhoArquivo) throws IOException {
        BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo));

        int linhas = Integer.parseInt(leitor.readLine().trim());
        int colunas = Integer.parseInt(leitor.readLine().trim());

        MalhaViaria malha = new MalhaViaria(linhas, colunas);

        for (int i = 0; i < linhas; i++) {
            String linha = leitor.readLine();
            if (linha == null) break;

            String[] valores = linha.split("\t");
            for (int j = 0; j < Math.min(valores.length, colunas); j++) {
                int valorDirecao = Integer.parseInt(valores[j].trim());
                malha.definirDirecaoQuadrante(i, j, valorDirecao);
            }
        }

        malha.estabelecerConexoes();

        leitor.close();
        return malha;
    }

    public void definirDirecaoQuadrante(int linha, int coluna, int valorDirecao) {
        Quadrante quadrante = quadrantes.get(criarChave(linha, coluna));
        if (quadrante != null) {
            Direcao direcao = Direcao.fromValor(valorDirecao);
            quadrante.setDirecao(direcao);
        }
    }

    // Método para estabelecer conexões após definir as direções de todos os quadrantes
    public void estabelecerConexoes() {
        for (Quadrante quadrante : quadrantes.values()) {
            if (quadrante.getDirecao() == Direcao.NADA) {
                continue;
            }

            // Só precisamos saber dos vizinhos seguintes, através das direções atuais, ex:
            // [7,3; Direcao.BAIXO] -> [7,4; Direcao.CRUZAMENTO_BAIXO_E_ESQUERDA] (a matriz é invertida, por isso Direcao.BAIXO AUMENTA Y, i.e. pula para a linha de baixo)
            // [7,4; Direcao.CRUZAMENTO_BAIXO_E_ESQUERDA] -> [7,5; Direcao.CRUZAMENTO_BAIXO_E_DIREITA] e [8,4; Direcao.ESQUERDA] (início de cruzamento - continua para o sul, ou para o leste)
            // Etc.

            // Se, no vizinho, (X v Y < 0) v (X > colunas) v (Y > linhas), define entrada ou saída da malha

            List<Direcao> direcoesPermitidas = obterDirecoesPermitidas(quadrante.getDirecao());

            for (Direcao dir : direcoesPermitidas) {
                Quadrante vizinho = encontrarQuadranteVizinho(quadrante, dir);

                if (vizinho != null) {
                    quadrante.adicionarVizinho(dir, vizinho);
                }
            }
        }
    }

    // TODO
    private List<Direcao> obterDirecoesPermitidas(Direcao direcao) {
        return null;
    }

    // Verifica se uma coordenada está dentro dos limites da malha
    private boolean coordenadaValida(int linha, int coluna) {
        return linha >= 0 && linha < linhas &&
                coluna >= 0 && coluna < colunas;
    }

    private Quadrante encontrarQuadranteVizinho(Quadrante origem, Direcao direcao) {
        int novaLinha = origem.getLinha();
        int novaColuna = origem.getColuna();

        switch (direcao) {
            case CIMA:
                novaLinha--;
                break;
            case DIREITA:
                novaColuna++;
                break;
            case BAIXO:
                novaLinha++;
                break;
            case ESQUERDA:
                novaColuna--;
                break;
            // TODO cases de nada e cruzamentos
            default:
                return null;
        }

        if (coordenadaValida(novaLinha, novaColuna)) {
            return quadrantes.get(criarChave(novaLinha, novaColuna));
        }
        return null;
    }

    public Quadrante getQuadrante(int linha, int coluna) {
        return quadrantes.get(criarChave(linha, coluna));
    }

}
