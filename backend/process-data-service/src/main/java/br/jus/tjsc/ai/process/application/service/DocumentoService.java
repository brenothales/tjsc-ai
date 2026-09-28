package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarDocumentosUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.Documento;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentoService implements BuscarDocumentosUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public DocumentoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public List<Documento> buscar(String numeroProcesso, int page, int size) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando documentos do processo {}: page={}, size={}", numero, page, size);
        List<Documento> docs = repository.buscarDocumentos(numero, page, size);
        log.debug("Documentos encontrados para {}: {}", numero, docs.size());
        return docs;
    }

    @Override
    public long contar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        return repository.contarDocumentos(numero);
    }
}
