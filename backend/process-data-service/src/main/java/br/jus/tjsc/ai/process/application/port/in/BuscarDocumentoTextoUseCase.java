package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;

import java.util.Optional;

public interface BuscarDocumentoTextoUseCase {
    Optional<DocumentoTexto> buscarPorId(String numeroProcesso, Long documentoId);
    Optional<DocumentoTexto> buscarSentenca(String numeroProcesso);
    Optional<DocumentoTexto> buscarPeticaoInicial(String numeroProcesso);
}
