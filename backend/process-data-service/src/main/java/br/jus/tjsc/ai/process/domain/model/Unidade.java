package br.jus.tjsc.ai.process.domain.model;

public record Unidade(
        Long id,
        String comarca,
        String vara,
        String dtIni,
        String status
) {}
