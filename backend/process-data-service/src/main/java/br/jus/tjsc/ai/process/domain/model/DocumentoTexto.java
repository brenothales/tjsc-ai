package br.jus.tjsc.ai.process.domain.model;

public record DocumentoTexto(
        Long id,
        Long processoId,
        String tipo,
        String autor,
        String dataJuntada,
        Integer numPaginas,
        String texto
) {}
