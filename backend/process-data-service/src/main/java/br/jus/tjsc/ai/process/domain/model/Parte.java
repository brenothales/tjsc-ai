package br.jus.tjsc.ai.process.domain.model;

public record Parte(
        Long id,
        String nome,
        String tipo,
        String documento,
        String dataCadastro,
        String polo
) {}
