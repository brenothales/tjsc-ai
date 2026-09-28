package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.PesquisarProcessosUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PesquisarProcessosService implements PesquisarProcessosUseCase {

    private static final Logger log = LoggerFactory.getLogger(PesquisarProcessosService.class);

    private final ProcessoRepository repository;

    public PesquisarProcessosService(ProcessoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Processo> pesquisar(String parte, String classe, String comarca, String situacao, int page, int size) {
        log.debug("Pesquisando processos: parte={}, classe={}, comarca={}, situacao={}", parte, classe, comarca, situacao);
        return repository.pesquisarProcessos(parte, classe, comarca, situacao, page, size);
    }

    @Override
    public long contar(String parte, String classe, String comarca, String situacao) {
        return repository.contarPesquisaProcessos(parte, classe, comarca, situacao);
    }
}
