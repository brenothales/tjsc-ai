package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarDocumentoTextoUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DocumentoTextoService implements BuscarDocumentoTextoUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentoTextoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public DocumentoTextoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public Optional<DocumentoTexto> buscarPorId(String numeroProcesso, Long documentoId) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando documento {} do processo {}", documentoId, numero);
        return repository.buscarDocumentoPorId(numero, documentoId);
    }

    @Override
    public Optional<DocumentoTexto> buscarSentenca(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando sentença do processo {}", numero);
        return repository.buscarSentencaComTexto(numero);
    }

    @Override
    public Optional<DocumentoTexto> buscarPeticaoInicial(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando petição inicial do processo {}", numero);
        return repository.buscarPeticaoInicialComTexto(numero);
    }
}
