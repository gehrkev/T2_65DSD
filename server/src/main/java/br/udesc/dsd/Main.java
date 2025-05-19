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
        String caminhoArquivo = Thread.currentThread().getContextClassLoader().getResource("malha-exemplo-1.txt").getPath();
        System.out.println("Caminho do arquivo: " + caminhoArquivo);
        MalhaController controller = new MalhaController(caminhoArquivo);

        System.out.println("Arquivo carregado de: " + caminhoArquivo);
        MalhaViaria malhaViaria = controller.getMalha();
        testarMalha(malhaViaria);
        MalhaView malhaView = new MalhaView(malhaViaria);

        Quadrante entrada = controller.getPontosDeEntrada().get(0);

        Carro carro = new Carro(entrada, 500, malhaView);
        carro.setName("Carro-1");

        try {
            entrada.getSemaforo().acquire();
            entrada.adicionarCarro(carro);
            entrada.setQuadranteDoCarro();

            carro.start();

            try {
                carro.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread principal interrompida enquanto aguardava o carro: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Não foi possível adicionar o carro: " + e.getMessage());
        }

        System.out.println("Carro finalizou o percurso.");
    }

    private static void testarMalha(MalhaViaria malha) {
        System.out.println("\nTestando alguns quadrantes:");

        Quadrante q00 = malha.getQuadrante(4, 15);
        System.out.println("Quadrante (" + q00.getLinha()+ "," + q00.getColuna() +"):");
        System.out.println("- Direção: " + q00.getDirecao());
        System.out.println("- Tem carro? " + q00.temCarro());
        System.out.println("- Vizinhos: " + q00.getVizinhosDaFrente().size());
        System.out.println("- É entrada: " + q00.isEntrada());

        System.out.println("- Direções possíveis: " + q00.getDirecoesPossiveis());

        System.out.println("\nTestando conexões:");
        q00.getVizinhosDaFrente().forEach((direcao, vizinho) -> {
            System.out.println("- Vizinho na direção " + direcao + ": (" +
                    vizinho.getLinha() + "," + vizinho.getColuna() +
                    ") com direção " + vizinho.getDirecao());
        });
    }

    private static void gerarNovoArquivo(MalhaViaria malha, String nomeArquivo) {
        try (PrintWriter writer = new PrintWriter(nomeArquivo)) {
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