package br.jus.tjsc.ai.agent.minuta.application;

public class MinutaNotFoundException extends RuntimeException {

    public MinutaNotFoundException(String numero, int versaoMinuta) {
        super("Minuta não encontrada: numero=%s versaoMinuta=%d".formatted(numero, versaoMinuta));
    }
}
