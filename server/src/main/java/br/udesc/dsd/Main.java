package br.udesc.dsd;

import br.udesc.dsd.controller.MalhaController;
import br.udesc.dsd.model.Carro;
import br.udesc.dsd.model.MalhaViaria;
import br.udesc.dsd.model.Quadrante;
import br.udesc.dsd.view.MalhaView;

import java.io.IOException;
import java.io.PrintWriter;

public class Main {
    public static void main(String[] args) throws IOException {
        // Carrega a malha do arquivo
        String caminhoArquivo = Thread.currentThread().getContextClassLoader().getResource("malha-exemplo-1.txt").getPath();
        System.out.println("Caminho do arquivo: " + caminhoArquivo);
        MalhaController controller = new MalhaController(caminhoArquivo);

        // Imprime informações da malha para teste
        System.out.println("Arquivo carregado de: " + caminhoArquivo);
        MalhaViaria malhaViaria = controller.getMalha();
        testarMalha(malhaViaria);
        MalhaView malhaView = new MalhaView(malhaViaria);

        Quadrante entrada = controller.getPontosDeEntrada().get(0);

        //Cria um carro
        Carro carro = new Carro(entrada, 500, malhaView);
        carro.setName("Carro-1");

        entrada.adicionarCarro(carro);
        entrada.setQuadranteDoCarro();

        carro.start();

        try {
            carro.join(); // aguarda a thread do carro terminar
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Carro finalizou o percurso.");
    }

    private static void testarMalha(MalhaViaria malha) {
        // Testa alguns quadrantes
        System.out.println("\nTestando alguns quadrantes:");

        // Testa quadrante específico
        Quadrante q00 = malha.getQuadrante(4, 15);
        System.out.println("Quadrante (" + q00.getLinha()+ "," + q00.getColuna() +"):");
        System.out.println("- Direção: " + q00.getDirecao());
        System.out.println("- Tem carro? " + q00.temCarro());
        System.out.println("- Vizinhos: " + q00.getVizinhosDaFrente().size());
        System.out.println("- É entrada: " + q00.isEntrada());

        // Imprime direções possíveis
        System.out.println("- Direções possíveis: " + q00.getDirecoesPossiveis());

        // Testa vizinhos
        System.out.println("\nTestando conexões:");
        q00.getVizinhosDaFrente().forEach((direcao, vizinho) -> {
            System.out.println("- Vizinho na direção " + direcao + ": (" +
                    vizinho.getLinha() + "," + vizinho.getColuna() +
                    ") com direção " + vizinho.getDirecao());
        });
    }

    private static void gerarNovoArquivo(MalhaViaria malha, String nomeArquivo) {
        try (PrintWriter writer = new PrintWriter(nomeArquivo)) {
            // Começa do quadrante (0,0) e vai linha por linha
            int linha = 0;
            int coluna = 0;
            Quadrante atual = malha.getQuadrante(0, 0);

            while (atual != null) {
                StringBuilder linhaSaida = new StringBuilder();
                coluna = 0;

                while ((atual = malha.getQuadrante(linha, coluna)) != null) {
                    if (coluna > 0) {
                        linhaSaida.append("\t");
                    }
                    linhaSaida.append(atual.getDirecao().getValor());
                    coluna++;
                }

                writer.println(linhaSaida.toString());
                linha++;
                atual = malha.getQuadrante(linha, 0);
            }

            System.out.println("\nNovo arquivo gerado: " + nomeArquivo);
        } catch (IOException e) {
            System.err.println("Erro ao gerar novo arquivo: " + e.getMessage());
        }
    }
}