package br.udesc.dsd.factory;

import br.udesc.dsd.controller.*;
import br.udesc.dsd.view.MalhaView;

public class ControllerFactory {
    public static ISimulacaoController criarSimulacaoController(
            MalhaController malhaController,
            MalhaView malhaView,
            boolean usarMonitor) {

        if (usarMonitor) {
            return new SimulacaoControllerMonitor(malhaController, malhaView);
        } else {
            return new SimulacaoControllerSemaforo(malhaController, malhaView);
        }
    }
}