package br.udesc.dsd.view;

import br.udesc.dsd.model.Direcao;
import br.udesc.dsd.model.MalhaViaria;
import br.udesc.dsd.model.Quadrante;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MalhaView {

    private final GridPane grid;
    private final VBox painelControles;
    private final HBox layoutPrincipal;
    private final MalhaViaria malha;

    public Spinner<Integer> limiteVeiculosSpinner;
    public Spinner<Integer> intervaloSpinner;
    public Button iniciarBotao;
    public Button encerrarInsercaoBotao;
    public Button encerrarSimulacaoBotao;
    public RadioButton monitorRadio;
    public RadioButton semaforoRadio;
    private final Label mensagemFinal;
    private final StackPane stackPane;


    public MalhaView(MalhaViaria malha) {
        this.malha = malha;
        this.grid = new GridPane();
        this.painelControles = construirPainelControles();
        desenharCelulas();

        mensagemFinal = new Label("SIMULAÇÃO ENCERRADA");
        mensagemFinal.setStyle("-fx-font-size: 24px; -fx-text-fill: #f14e4e;");
        mensagemFinal.setVisible(false);
        stackPane = new StackPane(grid, mensagemFinal);

        this.layoutPrincipal = new HBox(10, painelControles, stackPane);
    }

    public Pane getRoot() {
        return layoutPrincipal;
    }

    public void atualizarCelulas() {
        grid.getChildren().clear();
        desenharCelulas();
    }

    private void desenharCelulas() {
        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                Quadrante q = malha.getQuadrante(i, j);
                Rectangle cell = new Rectangle(30, 30);

                if (q.getDirecao() == Direcao.NADA) {
                    cell.setFill(Color.LIGHTGRAY);
                } else {
                    cell.setFill(q.temCarro() ? q.getCarro().getCor() : Color.WHITE);
                }

                cell.setStroke(Color.BLACK);
                grid.add(cell, j, i);
            }
        }
    }

    private VBox construirPainelControles() {
        limiteVeiculosSpinner = new Spinner<>(1, 100, 10);
        intervaloSpinner = new Spinner<>(100, 5000, 1000, 100);
        limiteVeiculosSpinner.setEditable(true);
        intervaloSpinner.setEditable(true);

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
                iniciarBotao, encerrarInsercaoBotao, encerrarSimulacaoBotao
        );
        painel.setPrefWidth(220);
        return painel;
    }

    public void exibirMensagemFinal(boolean exibir) {
        mensagemFinal.setVisible(exibir);
    }

}
