package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Advogado;

import java.util.List;

public interface BuscarAdvogadosUseCase {
    List<Advogado> buscar(String numeroProcesso);
}
