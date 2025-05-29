package br.udesc.dsd.view;

import br.udesc.dsd.model.Direcao;
import br.udesc.dsd.model.MalhaViaria;
import br.udesc.dsd.model.Quadrante;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class MalhaView {

    private final GridPane grid;
    private final Map<String, Rectangle> celulas = new HashMap<>();
    private final VBox painelControles;
    private final HBox layoutPrincipal;
    private MalhaViaria malha;

    public Spinner<Integer> limiteVeiculosSpinner;
    public Spinner<Integer> intervaloSpinner;
    public Button iniciarBotao;
    public Button encerrarInsercaoBotao;
    public Button encerrarSimulacaoBotao;
    public RadioButton monitorRadio;
    public RadioButton semaforoRadio;
    public ComboBox<String> comboBoxMalhas;
    private final Label mensagemFinal;
    private final StackPane stackPane;

    public MalhaView(MalhaViaria malha) {
        this.malha = malha;
        this.grid = new GridPane();
        this.painelControles = construirPainelControles();

        mensagemFinal = new Label("SIMULAÇÃO ENCERRADA");
        mensagemFinal.setStyle("-fx-font-size: 24px; -fx-text-fill: #f14e4e;");
        mensagemFinal.setVisible(false);
        stackPane = new StackPane(grid, mensagemFinal);

        this.layoutPrincipal = new HBox(10, painelControles, stackPane);
    }

    private VBox construirPainelControles() {
        limiteVeiculosSpinner = new Spinner<>(1, 100, 10);
        intervaloSpinner = new Spinner<>(100, 2000, 1000, 100);
        limiteVeiculosSpinner.setEditable(true);
        intervaloSpinner.setEditable(true);

        comboBoxMalhas = new ComboBox<>();
        comboBoxMalhas.setPromptText("Selecione a malha");
        carregarMalhasDisponiveis();

        iniciarBotao = new Button("Iniciar Simulação");
        encerrarInsercaoBotao = new Button("Encerrar Inserção");
        encerrarSimulacaoBotao = new Button("Encerrar Simulação");

        monitorRadio = new RadioButton("Monitor");
        semaforoRadio = new RadioButton("Semáforo");
        ToggleGroup grupoModo = new ToggleGroup();
        monitorRadio.setToggleGroup(grupoModo);
        semaforoRadio.setToggleGroup(grupoModo);
        semaforoRadio.setSelected(true);

        VBox painel = new VBox(10,
                new Label("Limite de veículos:"), limiteVeiculosSpinner,
                new Label("Intervalo de inserção (ms):"), intervaloSpinner,
                new Label("Mecanismo:"), monitorRadio, semaforoRadio,
                new Label("Arquivo de malha:"), comboBoxMalhas,
                iniciarBotao, encerrarInsercaoBotao, encerrarSimulacaoBotao
        );
        painel.setPrefWidth(220);
        return painel;
    }

    private void carregarMalhasDisponiveis() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            Enumeration<URL> resources = classLoader.getResources("malhas");
            Set<String> arquivosTxt = new HashSet<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File dir = new File(resource.toURI());
                if (dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
                    if (files != null) {
                        for (File file : files) {
                            arquivosTxt.add(file.getName());
                        }
                    }
                }
            }

            List<String> listaOrdenada = arquivosTxt.stream().sorted().collect(Collectors.toList());
            comboBoxMalhas.getItems().addAll(listaOrdenada);
            if (!listaOrdenada.isEmpty()) {
                comboBoxMalhas.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inicializarMalha(MalhaViaria malha) {
        this.malha = malha;
        grid.getChildren().clear();
        celulas.clear();

        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                Quadrante q = malha.getQuadrante(i, j);
                Rectangle cell = new Rectangle(30, 30);

                if (q.getDirecao() == Direcao.NADA) {
                    cell.setFill(Color.LIGHTGRAY);
                } else {
                    cell.setFill(Color.WHITE);
                }

                cell.setStroke(Color.BLACK);
                grid.add(cell, j, i);
                celulas.put(i + "," + j, cell);
            }
        }
    }

    public void atualizarCelulas() {
        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                Quadrante q = malha.getQuadrante(i, j);
                Rectangle cell = celulas.get(i + "," + j);

                if (cell != null && q.getDirecao() != Direcao.NADA) {
                    cell.setFill(q.temCarro() ? q.getCarro().getCor() : Color.WHITE);
                }
            }
        }
    }

    public void atualizarQuadrante(Quadrante q) {
        if (q == null) return;

        Rectangle cell = celulas.get(q.getLinha() + "," + q.getColuna());
        if (cell != null && q.getDirecao() != Direcao.NADA) {
            cell.setFill(q.temCarro() ? q.getCarro().getCor() : Color.WHITE);
        }
    }

    public void atualizarQuadrantes(Quadrante... quadrantes) {
        for (Quadrante q : quadrantes) {
            atualizarQuadrante(q);
        }
    }

    public Pane getRoot() {
        return layoutPrincipal;
    }

    public void exibirMensagemFinal(boolean exibir) {
        mensagemFinal.setVisible(exibir);
    }

    public void setMalha(MalhaViaria malha) {
        this.malha = malha;
    }
}
