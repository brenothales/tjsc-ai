package br.jus.tjsc.ai.agent.minuta.domain;

import java.util.List;

public record MinutaVersao(String tipo, String relatorio, String fundamentacao,
                           String dispositivo, List<MinutaStep> steps) {}
