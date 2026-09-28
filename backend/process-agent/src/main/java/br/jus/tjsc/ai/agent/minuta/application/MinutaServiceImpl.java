package br.jus.tjsc.ai.agent.minuta.application;

import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaDetailResponse;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaGerarRequest;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaResumoResponse;
import br.jus.tjsc.ai.agent.minuta.domain.MinutaDocument;
import br.jus.tjsc.ai.agent.minuta.infrastructure.MinutaRepository;
import br.jus.tjsc.ai.agent.minuta.infrastructure.ai.MinutaLoopService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
class MinutaServiceImpl implements MinutaService {

    private final MinutaLoopService minutaLoopService;
    private final MinutaRepository minutaRepository;

    MinutaServiceImpl(MinutaLoopService minutaLoopService, MinutaRepository minutaRepository) {
        this.minutaLoopService = minutaLoopService;
        this.minutaRepository  = minutaRepository;
    }

    @Override
    public Flux<String> gerar(MinutaGerarRequest request) {
        return minutaLoopService.gerarStream(request);
    }

    @Override
    public List<MinutaResumoResponse> historico(String numero) {
        return minutaRepository.findByNumeroOrderByVersaoMinutaDesc(numero)
                .stream()
                .map(this::toResumo)
                .toList();
    }

    @Override
    public MinutaDetailResponse buscarVersao(String numero, int versaoMinuta) {
        return minutaRepository.findByNumeroAndVersaoMinuta(numero, versaoMinuta)
                .map(this::toDetail)
                .orElseThrow(() -> new MinutaNotFoundException(numero, versaoMinuta));
    }

    private MinutaResumoResponse toResumo(MinutaDocument doc) {
        return new MinutaResumoResponse(
                doc.getId(), doc.getNumero(), doc.getVersaoMinuta(),
                doc.getCriadaEm(),
                doc.getVersoes() != null ? doc.getVersoes().size() : 0);
    }

    private MinutaDetailResponse toDetail(MinutaDocument doc) {
        return new MinutaDetailResponse(
                doc.getId(), doc.getNumero(), doc.getVersaoMinuta(),
                doc.getCriadaEm(), doc.getVersoes());
    }
}
