package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;

public record DocumentoTextoDto(
        Long id,
        String tipo,
        String autor,
        String dataJuntada,
        Integer numPaginas,
        String texto
) {
    public static DocumentoTextoDto from(DocumentoTexto d) {
        return new DocumentoTextoDto(
                d.id(), d.tipo(), d.autor(), d.dataJuntada(), d.numPaginas(), d.texto()
        );
    }
}
