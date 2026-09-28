package br.jus.tjsc.ai.agent.minuta.api.dto;

import java.util.List;

public record MinutaGerarRequest(String numero, List<String> versoes) {}
