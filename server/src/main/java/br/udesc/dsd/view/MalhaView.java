package br.udesc.dsd.view;

import br.udesc.dsd.model.Direcao;
import br.udesc.dsd.model.MalhaViaria;
import br.udesc.dsd.model.Quadrante;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MalhaView {

    private final GridPane grid;
    private final MalhaViaria malha;

    public MalhaView(MalhaViaria malha) {
        this.malha = malha;
        this.grid = new GridPane();

        desenharCelulas();
    }

    public GridPane getGrid() {
        return grid;
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
                    cell.setFill(Color.WHITE);
                    if (q.temCarro()) {
                        cell.setFill(Color.RED);
                    }
                }

                cell.setStroke(Color.BLACK);
                grid.add(cell, j, i);
            }
        }
    }
}
