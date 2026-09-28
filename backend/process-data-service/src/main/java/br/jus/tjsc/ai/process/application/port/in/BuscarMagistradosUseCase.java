package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Magistrado;

import java.util.List;

public interface BuscarMagistradosUseCase {
    List<Magistrado> buscar(String numeroProcesso);
}
