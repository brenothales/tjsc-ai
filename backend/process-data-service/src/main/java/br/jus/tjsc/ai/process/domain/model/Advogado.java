package br.jus.tjsc.ai.process.domain.model;

public record Advogado(
        Long documentoId,
        String nome,
        String oab,
        String tipoDocumento,
        String dataJuntada
) {}
