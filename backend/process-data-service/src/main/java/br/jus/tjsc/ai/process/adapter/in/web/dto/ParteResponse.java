package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Parte;

import java.util.List;

public record ParteResponse(
        boolean found,
        List<ParteDto> data,
        String message,
        List<Evidence> evidences
) {
    public static ParteResponse of(List<Parte> partes, String numero) {
        if (partes.isEmpty()) {
            return new ParteResponse(true, List.of(),
                    "Nenhuma parte foi encontrada para este processo.", List.of());
        }
        List<ParteDto> dtos = partes.stream().map(ParteDto::from).toList();
        List<Evidence> evs = partes.stream()
                .map(p -> Evidence.of("PARTE", p.id(), p.nome(), numero))
                .toList();
        return new ParteResponse(true, dtos, null, evs);
    }
}
