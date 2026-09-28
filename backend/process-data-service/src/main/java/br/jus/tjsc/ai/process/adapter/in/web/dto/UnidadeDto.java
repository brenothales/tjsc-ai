package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Unidade;

public record UnidadeDto(
        Long id,
        String comarca,
        String vara,
        String dtIni,
        String status
) {
    public static UnidadeDto from(Unidade u) {
        return new UnidadeDto(u.id(), u.comarca(), u.vara(), u.dtIni(), u.status());
    }
}
