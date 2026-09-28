package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.in.BuscarContextoUseCase;
import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.exception.ProcessoNaoEncontradoException;
import br.jus.tjsc.ai.process.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextoService implements BuscarContextoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ContextoService.class);

    private final ProcessoRepository repository;
    private final NumeroProcessoNormalizer normalizer;

    public ContextoService(ProcessoRepository repository, NumeroProcessoNormalizer normalizer) {
        this.repository = repository;
        this.normalizer = normalizer;
    }

    @Override
    public ContextoProcesso buscar(String numeroProcesso) {
        String numero = normalizer.normalizar(numeroProcesso);
        log.debug("Buscando contexto completo do processo {}", numero);

        Processo processo = repository.buscarPorNumero(numero)
                .orElseThrow(() -> new ProcessoNaoEncontradoException(numero));

        List<Parte> partes = repository.buscarPartes(numero);
        List<Advogado> advogados = repository.buscarAdvogados(numero);
        List<Magistrado> magistrados = repository.buscarMagistrados(numero);
        List<Movimentacao> movimentacoes = repository.buscarTodasMovimentacoes(numero);
        DocumentoTexto sentenca = repository.buscarSentencaComTexto(numero).orElse(null);
        DocumentoTexto peticaoInicial = repository.buscarPeticaoInicialComTexto(numero).orElse(null);
        Unidade unidade = processo.unidadeId() != null
                ? repository.buscarUnidade(processo.unidadeId()).orElse(null) : null;

        log.info("Contexto completo do processo {}: {} partes, {} movimentações, sentença={}",
                numero, partes.size(), movimentacoes.size(), sentenca != null);

        return new ContextoProcesso(processo, unidade, partes, advogados, magistrados,
                movimentacoes, sentenca, peticaoInicial);
    }

    public record ContextoProcesso(
            Processo processo,
            Unidade unidade,
            List<Parte> partes,
            List<Advogado> advogados,
            List<Magistrado> magistrados,
            List<Movimentacao> movimentacoes,
            DocumentoTexto sentenca,
            DocumentoTexto peticaoInicial
    ) {}
}
