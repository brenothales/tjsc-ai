package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Advogado;

public record AdvogadoDto(
        Long documentoId,
        String nome,
        String oab,
        String tipoDocumento,
        String dataJuntada
) {
    public static AdvogadoDto from(Advogado a) {
        return new AdvogadoDto(a.documentoId(), a.nome(), a.oab(), a.tipoDocumento(), a.dataJuntada());
    }
}
