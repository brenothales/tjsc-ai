package br.jus.tjsc.ai.agent.minuta.api;

import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaDetailResponse;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaGerarRequest;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaResumoResponse;
import br.jus.tjsc.ai.agent.minuta.application.MinutaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1/minuta")
public class MinutaController {

    private final MinutaService minutaService;

    MinutaController(MinutaService minutaService) {
        this.minutaService = minutaService;
    }

    @PostMapping(value = "/gerar", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> gerar(@RequestBody MinutaGerarRequest request) {
        return minutaService.gerar(request);
    }

    @GetMapping("/{numero}/historico")
    List<MinutaResumoResponse> historico(@PathVariable String numero) {
        return minutaService.historico(numero);
    }

    @GetMapping("/{numero}/{versaoMinuta}")
    MinutaDetailResponse getVersao(@PathVariable String numero,
                                   @PathVariable int versaoMinuta) {
        return minutaService.buscarVersao(numero, versaoMinuta);
    }
}
