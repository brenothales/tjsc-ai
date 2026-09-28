package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Parte;

import java.util.List;

public interface BuscarPartesUseCase {
    List<Parte> buscar(String numeroProcesso);
}
