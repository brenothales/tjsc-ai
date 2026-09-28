package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Magistrado;

import java.util.List;

public record MagistradoResponse(
        boolean found,
        List<MagistradoDto> data,
        String message,
        List<Evidence> evidences
) {
    public static MagistradoResponse of(List<Magistrado> magistrados, String numero) {
        if (magistrados.isEmpty()) {
            return new MagistradoResponse(true, List.of(),
                    "Nenhum magistrado foi encontrado para este processo.", List.of());
        }
        List<MagistradoDto> dtos = magistrados.stream().map(MagistradoDto::from).toList();
        List<Evidence> evs = magistrados.stream()
                .map(m -> Evidence.of("MAGISTRADO", m.id(), m.nome(), numero))
                .toList();
        return new MagistradoResponse(true, dtos, null, evs);
    }
}
