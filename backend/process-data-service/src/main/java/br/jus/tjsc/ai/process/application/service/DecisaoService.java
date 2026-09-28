package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarDecisaoUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.Decisao;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DecisaoService implements BuscarDecisaoUseCase {

    private static final Logger log = LoggerFactory.getLogger(DecisaoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public DecisaoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public Optional<Decisao> buscar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando decisão do processo: {}", numero);
        Optional<Decisao> decisao = repository.buscarDecisao(numero);
        log.debug("Decisão para {}: {}", numero, decisao.isPresent() ? "encontrada" : "não encontrada");
        return decisao;
    }
}
