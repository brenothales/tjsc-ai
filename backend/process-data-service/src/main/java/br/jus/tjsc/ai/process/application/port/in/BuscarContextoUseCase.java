package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.application.service.ContextoService;

public interface BuscarContextoUseCase {
    ContextoService.ContextoProcesso buscar(String numeroProcesso);
}
