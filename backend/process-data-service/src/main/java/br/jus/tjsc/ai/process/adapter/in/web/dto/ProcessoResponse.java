package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Processo;

public record ProcessoResponse(
        boolean found,
        ProcessoDto processo,
        String message,
        Evidence evidence
) {
    public static ProcessoResponse encontrado(Processo p) {
        ProcessoDto dto = ProcessoDto.from(p);
        Evidence ev = Evidence.of("PROCESSO", p.id(), p.classe(), p.numero());
        return new ProcessoResponse(true, dto, null, ev);
    }

    public static ProcessoResponse naoEncontrado(String numero) {
        return new ProcessoResponse(false, null,
                "Processo não encontrado nos dados disponíveis.", null);
    }
}
