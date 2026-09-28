package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Advogado;

import java.util.List;

public record AdvogadoResponse(
        boolean found,
        List<AdvogadoDto> data,
        String message,
        List<Evidence> evidences
) {
    public static AdvogadoResponse of(List<Advogado> advogados, String numero) {
        if (advogados.isEmpty()) {
            return new AdvogadoResponse(true, List.of(),
                    "Nenhum advogado foi encontrado para este processo.", List.of());
        }
        List<AdvogadoDto> dtos = advogados.stream().map(AdvogadoDto::from).toList();
        List<Evidence> evs = advogados.stream()
                .map(a -> Evidence.of("DOCUMENTO", a.documentoId(), a.nome(), numero))
                .toList();
        return new AdvogadoResponse(true, dtos, null, evs);
    }
}
