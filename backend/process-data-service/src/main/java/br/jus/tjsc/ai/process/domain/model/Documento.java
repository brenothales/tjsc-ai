package br.jus.tjsc.ai.process.domain.model;

public record Documento(
        Long id,
        Long processoId,
        String tipo,
        String autor,
        String dataJuntada,
        Integer numPaginas
) {}
