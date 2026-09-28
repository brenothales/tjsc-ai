package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarMagistradosUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.Magistrado;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MagistradoService implements BuscarMagistradosUseCase {

    private static final Logger log = LoggerFactory.getLogger(MagistradoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public MagistradoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public List<Magistrado> buscar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando magistrados do processo: {}", numero);
        List<Magistrado> magistrados = repository.buscarMagistrados(numero);
        log.debug("Magistrados encontrados para {}: {}", numero, magistrados.size());
        return magistrados;
    }
}
