package br.jus.tjsc.ai.process.domain.model;

public record Movimentacao(
        Long id,
        Long processoId,
        Integer codigo,
        String descricao,
        String dataMov
) {}
