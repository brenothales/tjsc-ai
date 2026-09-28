package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarDocumentosComTextoUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoComTextoService implements BuscarDocumentosComTextoUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentoComTextoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public DocumentoComTextoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public List<DocumentoTexto> buscar(String numeroProcesso, String tipo, int page, int size) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando documentos com texto: numero={}, tipo={}", numero, tipo);
        return repository.buscarDocumentosComTexto(numero, tipo, page, size);
    }

    @Override
    public long contar(String numeroProcesso, String tipo) {
        String numero = normalizer.normalizar(numeroProcesso);
        return repository.contarDocumentosComTexto(numero, tipo);
    }
}
