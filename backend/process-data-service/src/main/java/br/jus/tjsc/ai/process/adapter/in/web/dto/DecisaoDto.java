package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Decisao;

public record DecisaoDto(
        Long documentoId,
        String tipo,
        String autor,
        String data,
        Integer numPaginas
) {
    public static DecisaoDto from(Decisao d) {
        return new DecisaoDto(d.documentoId(), d.tipo(), d.autor(), d.data(), d.numPaginas());
    }
}
