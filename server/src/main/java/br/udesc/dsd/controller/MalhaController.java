package br.udesc.dsd.controller;

import br.udesc.dsd.model.Direcao;
import br.udesc.dsd.model.MalhaViaria;
import br.udesc.dsd.model.Quadrante;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class MalhaController {
    private MalhaViaria malha;

    public MalhaController(String caminhoArquivo) {
        malha = carregarDeArquivo(caminhoArquivo);
    }

    private MalhaViaria carregarDeArquivo(String caminhoArquivo) {
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo))) {
            int linhas = Integer.parseInt(leitor.readLine().trim());
            int colunas = Integer.parseInt(leitor.readLine().trim());

            MalhaViaria malha = new MalhaViaria(linhas, colunas);

            for (int i = 0; i < linhas; i++) {
                String linha = leitor.readLine();
                if (linha == null) break;

                String[] valores = linha.split("\t");
                for (int j = 0; j < Math.min(valores.length, colunas); j++) {
                    int valorDirecao = Integer.parseInt(valores[j].trim());
                    definirDirecaoQuadrante(i, j, valorDirecao);
                }
            }

            estabelecerPontosEntradaSaida();
            estabelecerConexoes();

            leitor.close();
            return malha;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erro ao carregar arquivo");
        }
        return null;
    }

    // TODO
    // Ler bordas da malha
    // 1) Linha 0: Se for 1, ponto de saída; se 3, ponto de entrada
    // 2) Coluna 0: Se for 4, ponto de saída; se for 2, ponto de entrada
    // 3) Linha final (int linhas - 1): Se for 3, ponto de saída; se for 1, ponto de entrada
    // 4) Coluna final (int colunas - 1
    private void estabelecerPontosEntradaSaida() {

    }

    private void definirDirecaoQuadrante(int linha, int coluna, int valorDirecao) {
        Quadrante quadrante = malha.getQuadrantes().get(criarChave(linha, coluna));
        if (quadrante != null) {
            Direcao direcao = Direcao.fromValor(valorDirecao);
            quadrante.setDirecao(direcao);
        }
    }

    // Método para estabelecer conexões (vizinhos da frente) após definir as direções de todos os quadrantes
    private void estabelecerConexoes() {
        for (Quadrante quadrante : malha.getQuadrantes().values()) {
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
                Quadrante vizinho = encontrarVizinhoDaFrente(quadrante, dir);

                if (vizinho != null) {
                    quadrante.adicionarVizinho(dir, vizinho);
                }
            }
        }
    }

    // TODO Checar
    private List<Direcao> obterDirecoesPermitidas(Direcao direcao) {
        return switch (direcao) {
            case CIMA, CRUZAMENTO_CIMA -> List.of(Direcao.CIMA);
            case BAIXO, CRUZAMENTO_BAIXO -> List.of(Direcao.BAIXO);
            case ESQUERDA, CRUZAMENTO_ESQUERDA -> List.of(Direcao.ESQUERDA);
            case DIREITA, CRUZAMENTO_DIREITA -> List.of(Direcao.DIREITA);
            case CRUZAMENTO_CIMA_E_DIREITA -> List.of(Direcao.CIMA, Direcao.DIREITA);
            case CRUZAMENTO_CIMA_E_ESQUERDA -> List.of(Direcao.CIMA, Direcao.ESQUERDA);
            case CRUZAMENTO_BAIXO_E_DIREITA -> List.of(Direcao.BAIXO, Direcao.DIREITA);
            case CRUZAMENTO_BAIXO_E_ESQUERDA -> List.of(Direcao.BAIXO, Direcao.ESQUERDA);
            default -> List.of(); // Lista vazia para quadrantes sem direção
        };
    }

    private Quadrante encontrarVizinhoDaFrente(Quadrante origem, Direcao direcao) {
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

            // TODO falta criar o vizinho
        }

        if (coordenadaValida(novaLinha, novaColuna)) {
            return malha.getQuadrantes().get(criarChave(novaLinha, novaColuna));
        }
        return null;
    }

    // Verifica se uma coordenada está dentro dos limites da malha
    private boolean coordenadaValida(int linha, int coluna) {
        return linha >= 0 && linha < malha.getLinhas() &&
                coluna >= 0 && coluna < malha.getColunas();
    }

    private String criarChave(int linha, int coluna) {
        return linha + "," + coluna;
    }

    public MalhaViaria getMalha() {
        return malha;
    }
}
