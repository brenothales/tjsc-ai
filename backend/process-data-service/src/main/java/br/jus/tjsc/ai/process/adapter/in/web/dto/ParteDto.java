package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Parte;

public record ParteDto(
        Long id,
        String nome,
        String tipo,
        String documento,
        String dataCadastro,
        String polo
) {
    public static ParteDto from(Parte p) {
        return new ParteDto(p.id(), p.nome(), p.tipo(), p.documento(), p.dataCadastro(), p.polo());
    }
}
