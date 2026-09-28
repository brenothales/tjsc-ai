package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Documento;

public record DocumentoDto(
        Long id,
        String tipo,
        String autor,
        String dataJuntada,
        Integer numPaginas
) {
    public static DocumentoDto from(Documento d) {
        return new DocumentoDto(d.id(), d.tipo(), d.autor(), d.dataJuntada(), d.numPaginas());
    }
}
