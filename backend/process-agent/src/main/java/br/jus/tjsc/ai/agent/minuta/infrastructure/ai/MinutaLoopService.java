package br.jus.tjsc.ai.agent.minuta.infrastructure.ai;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import br.jus.tjsc.ai.agent.minuta.api.dto.MinutaGerarRequest;
import br.jus.tjsc.ai.agent.minuta.domain.MinutaDocument;
import br.jus.tjsc.ai.agent.minuta.domain.MinutaStep;
import br.jus.tjsc.ai.agent.minuta.domain.MinutaVersao;
import br.jus.tjsc.ai.agent.minuta.domain.VersaoTipo;
import br.jus.tjsc.ai.agent.minuta.infrastructure.MinutaEventService;
import br.jus.tjsc.ai.agent.minuta.infrastructure.MinutaRepository;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class MinutaLoopService {

    private static final Logger log = LoggerFactory.getLogger(MinutaLoopService.class);

    static final int MAX_VERSOES = 5;
    static final int MAX_RETRIES = 2;

    private static final List<String> DEFAULT_VERSOES =
            List.of("procedente", "improcedente", "parcialmente procedente");

    private final ColetaDadosService coletaDadosService;
    private final RedacaoService     redacaoService;
    private final MinutaRepository   minutaRepository;
    private final MinutaEventService minutaEventService;
    private final ObjectMapper       objectMapper;

    MinutaLoopService(ColetaDadosService coletaDadosService,
                      RedacaoService redacaoService,
                      MinutaRepository minutaRepository,
                      MinutaEventService minutaEventService,
                      ObjectMapper objectMapper) {
        this.coletaDadosService = coletaDadosService;
        this.redacaoService     = redacaoService;
        this.minutaRepository   = minutaRepository;
        this.minutaEventService = minutaEventService;
        this.objectMapper       = objectMapper;
    }

    public Flux<String> gerarStream(MinutaGerarRequest request) {
        String sessionId = UUID.randomUUID().toString();
        Flux<String> flux = minutaEventService.criar(sessionId);
        Schedulers.boundedElastic().schedule(() -> executarLoop(sessionId, request));
        return flux;
    }

    private void executarLoop(String sessionId, MinutaGerarRequest request) {
        try {
            List<VersaoTipo> versoes = resolverVersoes(request.versoes());

            minutaEventService.publicar(sessionId, step("BUSCAR_DADOS",
                    "Coletando dados do processo " + request.numero() + "..."));
            log.info("[minuta] BUSCAR_DADOS numero={}", request.numero());

            String dados = coletaDadosService.coletar(request.numero());

            minutaEventService.publicar(sessionId, step("BUSCAR_DADOS", "Dados coletados com sucesso"));
            log.info("[minuta] dados coletados ({} chars)", dados.length());

            minutaEventService.publicar(sessionId, step("REDIGIR_RELATORIO", "Redigindo Relatório..."));

            String relatorio = comRetry(
                    () -> redacaoService.relatorio(request.numero(), dados));

            minutaEventService.publicar(sessionId, step("REDIGIR_RELATORIO", "Relatório redigido"));
            log.info("[minuta] relatório pronto ({} chars)", relatorio.length());


            int versaoMinuta = proximaVersao(request.numero());
            MinutaDocument doc = new MinutaDocument(
                    request.numero(), versaoMinuta, new ArrayList<>());
            minutaRepository.save(doc);
            log.info("[minuta] documento reservado: numero={} versaoMinuta={}",
                    request.numero(), versaoMinuta);


            for (VersaoTipo tipo : versoes) {
                log.info("[minuta] iniciando versão '{}'", tipo.label());

                List<MinutaStep> steps = new ArrayList<>(List.of(
                        new MinutaStep("BUSCAR_DADOS",      "Dados coletados"),
                        new MinutaStep("REDIGIR_RELATORIO", "Relatório redigido")
                ));

                minutaEventService.publicar(sessionId, step("REDIGIR_FUNDAMENTACAO",
                        "Redigindo Fundamentação (" + tipo.label() + ")..."));
                String fundamentacao = comRetry(
                        () -> redacaoService.fundamentacao(
                                request.numero(), tipo, dados, relatorio));
                steps.add(new MinutaStep("REDIGIR_FUNDAMENTACAO", "Fundamentação redigida"));

                minutaEventService.publicar(sessionId, step("REDIGIR_DISPOSITIVO",
                        "Redigindo Dispositivo (" + tipo.label() + ")..."));
                String dispositivo = comRetry(
                        () -> redacaoService.dispositivo(
                                request.numero(), tipo, dados, relatorio, fundamentacao));
                steps.add(new MinutaStep("REDIGIR_DISPOSITIVO", "Dispositivo redigido"));
                steps.add(new MinutaStep("FINISH",
                        "Versão '" + tipo.label() + "' concluída"));

                MinutaVersao versao = new MinutaVersao(
                        tipo.label(), relatorio, fundamentacao, dispositivo, steps);

                doc.getVersoes().add(versao);
                minutaRepository.save(doc);
                log.info("[minuta] versão '{}' salva (total={})",
                        tipo.label(), doc.getVersoes().size());

                minutaEventService.publicar(sessionId, versao(versao));
            }

            minutaEventService.publicar(sessionId, done(versaoMinuta));
            minutaEventService.concluir(sessionId);

        } catch (Exception e) {
            log.error("[minuta] erro durante geração", e);
            minutaEventService.erro(sessionId, e);
        }
    }


    private List<VersaoTipo> resolverVersoes(List<String> versoes) {
        List<String> efetivas = (versoes != null && !versoes.isEmpty()) ? versoes : DEFAULT_VERSOES;
        return efetivas.stream()
                .limit(MAX_VERSOES)
                .map(VersaoTipo::fromLabel)
                .toList();
    }

    private int proximaVersao(String numero) {
        return minutaRepository.findByNumeroOrderByVersaoMinutaDesc(numero)
                .stream().findFirst()
                .map(d -> d.getVersaoMinuta() + 1)
                .orElse(1);
    }

    String comRetry(Supplier<String> acao) {
        Exception ultima = null;
        for (int t = 1; t <= MAX_RETRIES; t++) {
            try {
                return acao.get();
            } catch (Exception e) {
                ultima = e;
                log.warn("[minuta] tentativa {}/{} falhou: {}", t, MAX_RETRIES, e.getMessage());
                if (t < MAX_RETRIES) {
                    try { Thread.sleep(1000L * t); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        throw new RuntimeException("Falha após " + MAX_RETRIES + " tentativas", ultima);
    }

    private String step(String acao, String mensagem) {
        return toJson(Map.of("evento", "step", "acao", acao, "mensagem", mensagem));
    }

    private String versao(MinutaVersao versao) {
        return toJson(Map.of("evento", "versao", "dados", versao));
    }

    private String done(int versaoMinuta) {
        return toJson(Map.of("evento", "done", "versaoMinuta", versaoMinuta));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            return "{\"evento\":\"erro\",\"mensagem\":\"Erro de serialização\"}";
        }
    }
}
