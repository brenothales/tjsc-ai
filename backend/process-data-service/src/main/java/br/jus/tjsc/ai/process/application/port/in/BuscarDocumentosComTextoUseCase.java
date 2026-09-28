package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;

import java.util.List;

public interface BuscarDocumentosComTextoUseCase {
    List<DocumentoTexto> buscar(String numeroProcesso, String tipo, int page, int size);
    long contar(String numeroProcesso, String tipo);
}
