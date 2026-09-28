package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Processo;

import java.util.Optional;

public interface BuscarProcessoUseCase {
    Optional<Processo> buscar(String numeroProcesso);
}
