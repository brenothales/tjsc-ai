package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Decisao;

public record DecisaoResponse(
        boolean found,
        DecisaoDto decisao,
        String message,
        Evidence evidence
) {
    public static DecisaoResponse encontrada(Decisao d, String numero) {
        return new DecisaoResponse(true, DecisaoDto.from(d), null,
                Evidence.of("DOCUMENTO", d.documentoId(), d.tipo(), numero));
    }

    public static DecisaoResponse naoEncontrada() {
        return new DecisaoResponse(false, null,
                "Nenhuma decisão foi encontrada para este processo.", null);
    }
}
