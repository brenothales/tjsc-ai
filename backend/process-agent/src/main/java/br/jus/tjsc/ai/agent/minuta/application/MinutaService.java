package br.jus.tjsc.ai.agent.minuta.application;

import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaDetailResponse;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaGerarRequest;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaResumoResponse;
import reactor.core.publisher.Flux;

import java.util.List;

public interface MinutaService {
    Flux<String> gerar(MinutaGerarRequest request);
    List<MinutaResumoResponse> historico(String numero);
    MinutaDetailResponse buscarVersao(String numero, int versaoMinuta);
}
