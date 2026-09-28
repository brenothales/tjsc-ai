package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarMovimentacoesUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.Movimentacao;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovimentacaoService implements BuscarMovimentacoesUseCase {

    private static final Logger log = LoggerFactory.getLogger(MovimentacaoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public MovimentacaoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public List<Movimentacao> buscar(String numeroProcesso, int page, int size) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando movimentações do processo {}: page={}, size={}", numero, page, size);
        List<Movimentacao> movs = repository.buscarMovimentacoes(numero, page, size);
        log.debug("Movimentações encontradas para {}: {}", numero, movs.size());
        return movs;
    }

    @Override
    public long contar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        return repository.contarMovimentacoes(numero);
    }
}
