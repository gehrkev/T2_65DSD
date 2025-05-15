package br.udesc.dsd.view;

import br.udesc.dsd.model.Direcao;
import br.udesc.dsd.model.MalhaViaria;
import br.udesc.dsd.model.Quadrante;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MalhaView {

    private final GridPane grid;

    public MalhaView(MalhaViaria malha) {
        this.grid = new GridPane();

        for (int i = 0; i < malha.getLinhas(); i++) {
            for (int j = 0; j < malha.getColunas(); j++) {
                Quadrante q = malha.getQuadrante(i, j);
                Rectangle cell = new Rectangle(30, 30);

                if (q.getDirecao() == Direcao.NADA) {
                    cell.setFill(Color.LIGHTGRAY);
                } else {
                    cell.setFill(Color.WHITE);
                    cell.setStroke(Color.BLACK);
                }

                grid.add(cell, j, i);
            }
        }
    }

    public GridPane getGrid() {
        return grid;
    }
}
