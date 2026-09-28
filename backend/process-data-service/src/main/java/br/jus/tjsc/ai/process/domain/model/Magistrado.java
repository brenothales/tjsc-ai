package br.jus.tjsc.ai.process.domain.model;

public record Magistrado(
        Long id,
        String nome,
        Long unidadeId,
        String dataInicio,
        String situacao
) {}
