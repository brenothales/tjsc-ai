package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;

public record DocumentoTextoResponse(
        boolean found,
        DocumentoTextoDto documento,
        String message,
        Evidence evidence
) {
    public static DocumentoTextoResponse encontrado(DocumentoTexto d, String numero) {
        return new DocumentoTextoResponse(
                true,
                DocumentoTextoDto.from(d),
                null,
                Evidence.of("DOCUMENTO", d.id(), d.tipo(), numero)
        );
    }

    public static DocumentoTextoResponse naoEncontrado(String mensagem) {
        return new DocumentoTextoResponse(false, null, mensagem, null);
    }
}
