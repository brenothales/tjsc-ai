package br.jus.tjsc.ai.agent.minuta.api.dto;

import br.jus.tjsc.ai.agent.minuta.domain.MinutaVersao;

import java.time.Instant;
import java.util.List;

public record MinutaDetailResponse(String id, String numero, int versaoMinuta,
                                   Instant criadaEm, List<MinutaVersao> versoes) {}
