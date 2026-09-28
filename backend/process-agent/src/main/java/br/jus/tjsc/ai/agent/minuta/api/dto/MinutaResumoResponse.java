package br.jus.tjsc.ai.agent.minuta.api.dto;

import java.time.Instant;

public record MinutaResumoResponse(String id, String numero, int versaoMinuta,
                                   Instant criadaEm, int qtdVersoes) {}
