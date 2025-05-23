package br.udesc.dsd.factory;

import br.udesc.dsd.controller.*;
import br.udesc.dsd.view.MalhaView;

public class ControllerFactory {
    public static ISimulacaoController criarSimulacaoController(
            MalhaController malhaController,
            MalhaView malhaView,
            boolean usarMonitor) {

        if (usarMonitor) {
            throw new UnsupportedOperationException("Monitor ainda não implementado");
        } else {
            return new SimulacaoControllerSemaforo(malhaController, malhaView);
        }
    }
}