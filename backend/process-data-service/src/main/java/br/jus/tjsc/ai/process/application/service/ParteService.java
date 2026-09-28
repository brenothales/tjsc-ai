package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarPartesUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import br.jus.tjsc.ai.process.domain.model.Parte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParteService implements BuscarPartesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParteService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public ParteService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public List<Parte> buscar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando partes do processo: {}", numero);
        List<Parte> partes = repository.buscarPartes(numero);
        log.debug("Partes encontradas para {}: {}", numero, partes.size());
        return partes;
    }
}
