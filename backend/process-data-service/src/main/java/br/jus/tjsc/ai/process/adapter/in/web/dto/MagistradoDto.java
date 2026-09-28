package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Magistrado;

public record MagistradoDto(
        Long id,
        String nome,
        String dataInicio,
        String situacao
) {
    public static MagistradoDto from(Magistrado m) {
        return new MagistradoDto(m.id(), m.nome(), m.dataInicio(), m.situacao());
    }
}
