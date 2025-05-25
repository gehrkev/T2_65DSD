package br.udesc.dsd.model;

import br.udesc.dsd.view.MalhaView;
import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.util.*;

public class CarroMonitor extends Thread implements ICarro {
    private Quadrante quadranteAtual;
    private final long velocidade;
    private final MalhaView malhaView;
    private final Color cor;
    private static final Color[] CORES_DISPONIVEIS = {
            Color.ORANGERED, Color.STEELBLUE, Color.FORESTGREEN, Color.DARKORANGE,
            Color.MEDIUMPURPLE, Color.GOLDENROD, Color.DEEPPINK, Color.LIGHTSKYBLUE
    };

    private volatile boolean ativo = true;
    private volatile boolean shutdownRequested = false;
    private Runnable onTermino;
    private final Random rand = new Random();

    public CarroMonitor(Quadrante quadranteInicial, long velocidade, MalhaView malhaView) {
        this.quadranteAtual = quadranteInicial;
        this.velocidade = velocidade;
        this.malhaView = malhaView;
        this.cor = CORES_DISPONIVEIS[rand.nextInt(CORES_DISPONIVEIS.length)];
        this.setName("Carro-" + System.currentTimeMillis());
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500); // Para dar tempo da interface atualizar

            while (ativo && !shutdownRequested) {
                Quadrante atual = this.quadranteAtual;
                Direcao direcao = atual.getDirecao();

                Quadrante proximo = atual.getVizinho(direcao);
                if (proximo == null) {
                    System.out.println(getName() + " saiu da malha. Encerrando thread.");
                    break;
                }

                boolean entrandoEmCruzamento = !isCruzamento(atual.getDirecao()) && isCruzamento(proximo.getDirecao());

                if (entrandoEmCruzamento) {
                    List<Quadrante> saidasPossiveis = coletarSaidasPossiveis(atual);

                    if (saidasPossiveis.isEmpty()) {
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }

                    Quadrante saidaEscolhida = saidasPossiveis.get(rand.nextInt(saidasPossiveis.size()));
                    List<Quadrante> caminho = encontrarCaminhoParaSaida(atual, saidaEscolhida);

                    if (caminho.isEmpty()) {
                        Thread.sleep(rand.nextInt(500));
                        continue;
                    }

                    if (reservarCaminhoMonitor(caminho)) {
                        atravessarCruzamentoMonitor(caminho);
                    } else {
                        Thread.sleep(rand.nextInt(500));
                    }
                } else {
                    if (moverParaQuadranteMonitor(proximo)) {
                        System.out.println(getName() + " movido para: " + proximo);
                    } else {
                        Thread.sleep(rand.nextInt(500));
                    }
                }

                Thread.sleep(velocidade);
            }
        } catch (InterruptedException e) {
            if (!shutdownRequested) {
                System.out.println(getName() + " interrompido inesperadamente.");
            }
        } finally {
            cleanup();
        }

        if (onTermino != null) onTermino.run();
    }

    private boolean moverParaQuadranteMonitor(Quadrante proximo) throws InterruptedException {
        synchronized (proximo) {
            while (proximo.getCarro() != null) {
                proximo.wait();
            }
            Quadrante atual = this.quadranteAtual;

            synchronized (atual) {
                atual.setCarro(null);
                proximo.setCarro(this);
                this.quadranteAtual = proximo;
                atual.notifyAll();
            }

            Platform.runLater(() -> malhaView.atualizarQuadrantes(atual, proximo));
            return true;
        }
    }

    private boolean reservarCaminhoMonitor(List<Quadrante> caminho) throws InterruptedException {
        List<Quadrante> ordenados = new ArrayList<>(caminho);
        ordenados.sort(Comparator.comparingInt(Object::hashCode));

        while (true) {
            List<Quadrante> adquiridos = new ArrayList<>();

            try {
                for (Quadrante q : ordenados) {
                    synchronized (q) {
                        adquiridos.add(q);
                        if (q.getCarro() != null) {
                            break;
                        }
                    }
                }

                if (adquiridos.size() == ordenados.size()) {
                    return true;
                }
            } finally {
                for (Quadrante q : adquiridos) {
                    synchronized (q) {
                        q.notifyAll();
                    }
                }
            }

            Thread.sleep(50 + rand.nextInt(100));
        }
    }

    private void atravessarCruzamentoMonitor(List<Quadrante> caminho) throws InterruptedException {
        for (int i = 0; i < caminho.size(); i++) {
            Quadrante proximo = caminho.get(i);
            Quadrante atual = this.quadranteAtual;

            Object primeiroLock = atual.hashCode() < proximo.hashCode() ? atual : proximo;
            Object segundoLock = atual.hashCode() < proximo.hashCode() ? proximo : atual;

            synchronized (primeiroLock) {
                synchronized (segundoLock) {
                    while (proximo.getCarro() != null) {
                        proximo.wait();
                    }

                    atual.setCarro(null);
                    proximo.setCarro(this);
                    this.quadranteAtual = proximo;

                    atual.notifyAll();
                    proximo.notifyAll();
                }
            }

            Platform.runLater(() -> malhaView.atualizarQuadrantes(atual, proximo));
            Thread.sleep(velocidade);
        }
    }

    private boolean isCruzamento(Direcao direcao) {
        return direcao == Direcao.CRUZAMENTO_CIMA ||
                direcao == Direcao.CRUZAMENTO_BAIXO ||
                direcao == Direcao.CRUZAMENTO_ESQUERDA ||
                direcao == Direcao.CRUZAMENTO_DIREITA ||
                direcao == Direcao.CRUZAMENTO_CIMA_E_DIREITA ||
                direcao == Direcao.CRUZAMENTO_CIMA_E_ESQUERDA ||
                direcao == Direcao.CRUZAMENTO_BAIXO_E_DIREITA ||
                direcao == Direcao.CRUZAMENTO_BAIXO_E_ESQUERDA;
    }

    private List<Quadrante> coletarSaidasPossiveis(Quadrante entrada) {
        List<Quadrante> saidas = new ArrayList<>();
        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();

        Quadrante inicio = entrada.getVizinho(entrada.getDirecao());
        if (inicio == null) return saidas;

        fila.add(inicio);
        visitados.add(inicio);

        while (!fila.isEmpty()) {
            Quadrante atual = fila.poll();

            for (Direcao d : atual.getDirecoesPossiveis()) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho == null || visitados.contains(vizinho)) continue;

                visitados.add(vizinho);

                if (isCruzamento(vizinho.getDirecao())) {
                    fila.add(vizinho);
                } else {
                    saidas.add(vizinho);
                }
            }
        }

        return saidas;
    }

    private List<Quadrante> encontrarCaminhoParaSaida(Quadrante entrada, Quadrante saidaAlvo) {
        List<Quadrante> caminho = new ArrayList<>();
        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();
        Map<Quadrante, Quadrante> veioDe = new HashMap<>();

        Quadrante inicio = entrada.getVizinho(entrada.getDirecao());
        if (inicio == null) return caminho;

        fila.add(inicio);
        visitados.add(inicio);
        veioDe.put(inicio, null);

        boolean encontrouSaida = false;

        while (!fila.isEmpty() && !encontrouSaida) {
            Quadrante atual = fila.poll();

            for (Direcao d : atual.getDirecoesPossiveis()) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho == null || visitados.contains(vizinho)) continue;

                visitados.add(vizinho);
                veioDe.put(vizinho, atual);

                if (vizinho == saidaAlvo) {
                    encontrouSaida = true;
                    break;
                }

                if (isCruzamento(vizinho.getDirecao())) {
                    fila.add(vizinho);
                }
            }
        }

        if (!encontrouSaida) return caminho;

        Quadrante atual = saidaAlvo;
        while (atual != null && veioDe.get(atual) != null) {
            caminho.add(0, atual);
            atual = veioDe.get(atual);
        }

        if (atual != null) caminho.add(0, atual);

        return caminho;
    }

    private void cleanup() {
        if (quadranteAtual != null) {
            Quadrante ultimo = quadranteAtual;
            synchronized (ultimo) {
                ultimo.setCarro(null);
                ultimo.notifyAll();
            }
            Platform.runLater(() -> malhaView.atualizarQuadrante(ultimo));
        }
        System.out.println(getName() + " finalizou corretamente.");
    }

    public void requestShutdown() {
        this.shutdownRequested = true;
        this.ativo = false;
        this.interrupt();
    }

    public Color getCor() {
        return cor;
    }

    public void setQuadranteAtual(Quadrante quadrante) {
        this.quadranteAtual = quadrante;
    }

    public void setOnTermino(Runnable onTermino) {
        this.onTermino = onTermino;
    }
}
