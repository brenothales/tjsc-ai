package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Movimentacao;

public record MovimentacaoDto(
        Long id,
        Integer codigo,
        String descricao,
        String dataMov
) {
    public static MovimentacaoDto from(Movimentacao m) {
        return new MovimentacaoDto(m.id(), m.codigo(), m.descricao(), m.dataMov());
    }
}
