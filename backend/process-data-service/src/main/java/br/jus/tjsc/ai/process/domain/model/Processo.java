package br.jus.tjsc.ai.process.domain.model;

import java.math.BigDecimal;

public record Processo(
        Long id,
        String numero,
        String classe,
        String assunto,
        String comarca,
        Long unidadeId,
        Long magistradoId,
        String dataAutuacao,
        String dataSentenca,
        BigDecimal valorCausa,
        String situacao
) {}
