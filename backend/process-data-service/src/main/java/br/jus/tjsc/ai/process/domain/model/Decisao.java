package br.jus.tjsc.ai.process.domain.model;

public record Decisao(
        Long documentoId,
        Long processoId,
        String tipo,
        String autor,
        String data,
        Integer numPaginas
) {}
