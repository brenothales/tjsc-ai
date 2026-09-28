package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Documento;

import java.util.List;

public interface BuscarDocumentosUseCase {
    List<Documento> buscar(String numeroProcesso, int page, int size);
    long contar(String numeroProcesso);
}
