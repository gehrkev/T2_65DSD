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
        this.setName("Carro-" + System.currentTimeMillis() + "-" + rand.nextInt(1000));
        if (quadranteInicial != null) {
            synchronized (quadranteInicial) {
                quadranteInicial.setCarro(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500 + rand.nextInt(500));

            while (ativo && !shutdownRequested) {
                Quadrante atual = this.quadranteAtual;
                if (atual == null) {
                    System.out.println(getName() + " está sem quadrante atual. Encerrando.");
                    break;
                }
                Direcao direcao = atual.getDirecao();

                Quadrante proximo = atual.getVizinho(direcao);

                boolean entrandoEmCruzamento = !isCruzamento(atual.getDirecao()) && proximo != null && isCruzamento(proximo.getDirecao());

                if (entrandoEmCruzamento) {
                    System.out.println(getName() + " em " + atual + " tentando entrar no cruzamento via " + proximo);
                    List<Quadrante> saidasPossiveis = coletarSaidasPossiveis(proximo);

                    if (saidasPossiveis.isEmpty()) {
                        System.out.println(getName() + " não encontrou saídas possíveis do cruzamento " + proximo + ". Aguardando.");
                        Thread.sleep(velocidade + rand.nextInt(250));
                        continue;
                    }

                    Quadrante saidaEscolhida = saidasPossiveis.get(rand.nextInt(saidasPossiveis.size()));
                    System.out.println(getName() + " escolheu sair por " + saidaEscolhida);

                    List<Quadrante> caminhoNoCruzamento = encontrarCaminhoParaSaida(proximo, saidaEscolhida);

                    if (caminhoNoCruzamento.isEmpty() || caminhoNoCruzamento.get(0) != proximo) {
                        if (caminhoNoCruzamento.isEmpty() || (proximo != null && !caminhoNoCruzamento.contains(proximo))) {
                            List<Quadrante> tempPath = new ArrayList<>();
                            if (proximo != null) tempPath.add(proximo);
                            tempPath.addAll(caminhoNoCruzamento);
                            caminhoNoCruzamento = tempPath;
                        }
                        if (caminhoNoCruzamento.isEmpty()) {
                            System.out.println(getName() + " não encontrou caminho para " + saidaEscolhida + " a partir de " + proximo + ". Aguardando.");
                            Thread.sleep(velocidade + rand.nextInt(250));
                            continue;
                        }
                    }

                    System.out.println(getName() + " caminho no cruzamento: " + caminhoNoCruzamento);

                    if (reservarCaminho(caminhoNoCruzamento)) {
                        System.out.println(getName() + " reservou com sucesso o caminho: " + caminhoNoCruzamento);
                        atravessarCruzamento(caminhoNoCruzamento);
                        System.out.println(getName() + " atravessou o cruzamento. Agora em: " + this.quadranteAtual);
                    } else {
                        System.out.println(getName() + " falhou ao reservar caminho " + caminhoNoCruzamento + ". Tentando novamente mais tarde.");
                        Thread.sleep(velocidade + rand.nextInt(250));
                    }
                } else {
                    if (proximo == null) {
                        System.out.println(getName() + " em " + atual + " saiu da malha. Encerrando thread.");
                        ativo = false;
                        break;
                    }
                    if (!moverParaQuadrante(proximo)) {
                        System.out.println(getName() + " não conseguiu mover de " + atual + " para " + proximo + ". Aguardando.");
                        Thread.sleep(velocidade + rand.nextInt(150));
                    } else {
                        System.out.println(getName() + " movido de " + atual + " para " + proximo);
                        Thread.sleep(velocidade);
                    }
                }
            }
        } catch (InterruptedException e) {
            if (!shutdownRequested) {
                System.out.println(getName() + " interrompido inesperadamente.");
                Thread.currentThread().interrupt();
            } else {
                System.out.println(getName() + " interrompido para shutdown.");
            }
        } catch (Exception e) {
            System.err.println(getName() + " encontrou uma exceção inesperada: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            cleanup();
        }

        if (onTermino != null) {
            try {
                onTermino.run();
            } catch (Exception e){
                System.err.println(getName() + " exceção no callback onTermino: " + e.getMessage());
            }
        }
        System.out.println(getName() + " thread finalizada.");
    }

    /**
     * Move o carro para o próximo quadrante.
     * Este método usa ordenação de locks e não usa wait() enquanto mantém múltiplos locks.
     * Retorna false se não conseguir adquirir o lock ou se o próximo quadrante estiver ocupado.
     */
    private boolean moverParaQuadrante(Quadrante proximo) throws InterruptedException {
        Quadrante atual = this.quadranteAtual;
        if (atual == proximo) return true;

        Object lock1 = System.identityHashCode(atual) < System.identityHashCode(proximo) ? atual : proximo;
        Object lock2 = System.identityHashCode(atual) < System.identityHashCode(proximo) ? proximo : atual;

        synchronized (lock1) {
            if (shutdownRequested) return false;
            synchronized (lock2) {
                if (shutdownRequested) return false;

                if (proximo.getCarro() == null || proximo.getCarro() == this) {
                    if (this.quadranteAtual == atual) {
                        atual.setCarro(null);
                    } else {
                        System.err.println(getName() + " estado inconsistente em moverParaQuadrante: quadranteAtual mudou.");
                        return false;
                    }

                    proximo.setCarro(this);
                    this.quadranteAtual = proximo;

                    atual.notifyAll();

                    final Quadrante finalAtual = atual;
                    final Quadrante finalProximo = proximo;
                    Platform.runLater(() -> malhaView.atualizarQuadrantes(finalAtual, finalProximo));
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * "Reserva" um caminho verificando se todos os quadrantes estão livres.
     * Este método adquire locks em uma ordem definida (hashCode) para verificar.
     * IMPORTANTE: NÃO mantém os locks após retornar true. O caminho pode ser tomado por outros.
     * É mais uma verificação "o caminho está livre agora?".
     */
    private boolean reservarCaminho(List<Quadrante> caminho) throws InterruptedException {
        if (caminho == null || caminho.isEmpty()) {
            return false;
        }
        List<Quadrante> ordenados = new ArrayList<>(caminho);
        ordenados.sort(Comparator.comparingInt(System::identityHashCode));

        List<Quadrante> locksTemporariamenteAdquiridos = new ArrayList<>();
        try {
            for (Quadrante q : ordenados) {
                if (shutdownRequested) return false;
                synchronized (q) {
                    locksTemporariamenteAdquiridos.add(q);
                    if (q.getCarro() != null && q.getCarro() != this) {
                        return false;
                    }
                }
            }
            return true;
        } finally {
            for (Quadrante qLock : locksTemporariamenteAdquiridos) {
                if (shutdownRequested) break;
                synchronized(qLock) {
                    qLock.notifyAll();
                }
            }
        }
    }

    /**
     * Percorre um caminho pré-determinado, tipicamente num cruzamento.
     * Tenta mover passo-a-passo, tentando novamente se um segmento estiver bloqueado.
     */
    private void atravessarCruzamento(List<Quadrante> caminho) throws InterruptedException {
        if (caminho == null) return;

        for (Quadrante proximoPasso : caminho) {
            if (shutdownRequested) break;
            if (this.quadranteAtual == proximoPasso) {
                System.out.println(getName() + " já está em " + proximoPasso + " no caminho do cruzamento.");
                Thread.sleep(velocidade);
                continue;
            }

            boolean movidoComSucesso = false;
            int tentativas = 0;
            int maxTentativas = 50;

            while (!movidoComSucesso && !shutdownRequested && tentativas < maxTentativas) {
                if (moverParaQuadrante(proximoPasso)) {
                    movidoComSucesso = true;
                    System.out.println(getName() + " atravessando cruzamento, movido para: " + proximoPasso);
                    Thread.sleep(velocidade);
                } else {
                    tentativas++;
                    Thread.sleep(50 + rand.nextInt(100));
                }
            }

            if (!movidoComSucesso && !shutdownRequested) {
                System.out.println(getName() + " falhou em mover para " + proximoPasso + " no cruzamento após " + maxTentativas + " tentativas. Saindo do cruzamento.");
                break;
            }
            if (shutdownRequested) break;
        }
    }

    private boolean isCruzamento(Direcao direcao) {
        if (direcao == null) return false;
        return direcao == Direcao.CRUZAMENTO_CIMA ||
                direcao == Direcao.CRUZAMENTO_BAIXO ||
                direcao == Direcao.CRUZAMENTO_ESQUERDA ||
                direcao == Direcao.CRUZAMENTO_DIREITA ||
                direcao == Direcao.CRUZAMENTO_CIMA_E_DIREITA ||
                direcao == Direcao.CRUZAMENTO_CIMA_E_ESQUERDA ||
                direcao == Direcao.CRUZAMENTO_BAIXO_E_DIREITA ||
                direcao == Direcao.CRUZAMENTO_BAIXO_E_ESQUERDA;
    }

    private List<Quadrante> coletarSaidasPossiveis(Quadrante entradaCruzamento) {
        List<Quadrante> saidas = new ArrayList<>();
        if (entradaCruzamento == null || !isCruzamento(entradaCruzamento.getDirecao())) {
            return saidas;
        }

        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();

        fila.add(entradaCruzamento);
        visitados.add(entradaCruzamento);

        while (!fila.isEmpty() && !shutdownRequested) {
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

    private List<Quadrante> encontrarCaminhoParaSaida(Quadrante inicioCruzamento, Quadrante saidaAlvo) {
        List<Quadrante> caminho = new ArrayList<>();
        if (inicioCruzamento == null || saidaAlvo == null || !isCruzamento(inicioCruzamento.getDirecao())) {
            return caminho;
        }

        Set<Quadrante> visitados = new HashSet<>();
        Queue<Quadrante> fila = new LinkedList<>();
        Map<Quadrante, Quadrante> veioDe = new HashMap<>();

        fila.add(inicioCruzamento);
        visitados.add(inicioCruzamento);
        veioDe.put(inicioCruzamento, null);

        Quadrante encontrado = null;

        while (!fila.isEmpty() && !shutdownRequested) {
            Quadrante atual = fila.poll();

            if (atual == saidaAlvo) {
                encontrado = atual;
                break;
            }

            if (!isCruzamento(atual.getDirecao()) && atual != saidaAlvo) {
                continue;
            }

            for (Direcao d : atual.getDirecoesPossiveis()) {
                Quadrante vizinho = atual.getVizinho(d);
                if (vizinho == null || visitados.contains(vizinho)) continue;

                if (!isCruzamento(vizinho.getDirecao()) && vizinho != saidaAlvo) {
                    visitados.add(vizinho);
                    veioDe.put(vizinho, atual);
                    if (vizinho == saidaAlvo) {
                        encontrado = vizinho;
                        break;
                    }
                    continue;
                }

                visitados.add(vizinho);
                veioDe.put(vizinho, atual);
                fila.add(vizinho);

                if (vizinho == saidaAlvo) {
                    encontrado = vizinho;
                    break;
                }
            }
            if (encontrado != null) break;
        }

        if (encontrado != null) {
            Quadrante passo = encontrado;
            while (passo != null) {
                caminho.add(0, passo);
                if (passo == inicioCruzamento) break;
                passo = veioDe.get(passo);
                if (passo != null && !visitados.contains(passo) && passo != inicioCruzamento) {
                    System.err.println(getName() + ": Erro ao reconstruir caminho, loop ou nó não visitado.");
                    return new ArrayList<>();
                }
            }
        }

        if (!caminho.isEmpty() && caminho.get(0) != inicioCruzamento && encontrado != null) {
            if (inicioCruzamento == saidaAlvo) {
                caminho.clear();
                caminho.add(inicioCruzamento);
            } else {
                System.err.println(getName() + ": Caminho reconstruído não inicia com o quadrante de entrada do cruzamento.");
            }
        }
        return caminho;
    }

    private void cleanup() {
        Quadrante ultimoQuadrante = this.quadranteAtual;
        if (ultimoQuadrante != null) {
            synchronized (ultimoQuadrante) {
                if (ultimoQuadrante.getCarro() == this) {
                    ultimoQuadrante.setCarro(null);
                    ultimoQuadrante.notifyAll();
                }
            }
            final Quadrante finalUltimo = ultimoQuadrante;
            Platform.runLater(() -> {
                if (finalUltimo != null) malhaView.atualizarQuadrante(finalUltimo);
            });
        }
        this.quadranteAtual = null;
        System.out.println(getName() + " cleanup executado.");
    }

    public void requestShutdown() {
        System.out.println(getName() + " shutdown solicitado.");
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

    public String getCarName() {
        return getName();
    }
}