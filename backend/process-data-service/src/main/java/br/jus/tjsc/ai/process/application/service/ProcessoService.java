package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarProcessoUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import br.jus.tjsc.ai.process.domain.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcessoService implements BuscarProcessoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public ProcessoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public Optional<Processo> buscar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando processo: {}", numero);
        Optional<Processo> resultado = repository.buscarPorNumero(numero);
        log.debug("Processo {}: {}", numero, resultado.isPresent() ? "encontrado" : "não encontrado");
        return resultado;
    }
}
