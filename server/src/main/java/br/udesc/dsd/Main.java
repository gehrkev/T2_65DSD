package br.udesc.dsd;

import br.udesc.dsd.model.MalhaViaria;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws IOException {
        String caminho = new String(); // pegar através de um arg java ex: java -jar target/...snapshot.jar ./caminhoDaMalha/arquivo.txt
        MalhaViaria.carregarDeArquivo(caminho);
    }
}
