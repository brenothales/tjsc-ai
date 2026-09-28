package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Decisao;

import java.util.Optional;

public interface BuscarDecisaoUseCase {
    Optional<Decisao> buscar(String numeroProcesso);
}
