package br.jus.tjsc.ai.process.adapter.in.web;

import br.jus.tjsc.ai.process.adapter.in.web.dto.*;
import br.jus.tjsc.ai.process.application.port.in.*;
import br.jus.tjsc.ai.process.domain.model.Documento;
import br.jus.tjsc.ai.process.domain.model.DocumentoTexto;
import br.jus.tjsc.ai.process.domain.model.Movimentacao;
import br.jus.tjsc.ai.process.domain.model.Processo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/processos")
@Validated
@Tag(name = "Processos", description = "Consulta de dados processuais do TJSC")
public class ProcessoController {

    private static final Logger log = LoggerFactory.getLogger(ProcessoController.class);

    private final BuscarProcessoUseCase buscarProcesso;
    private final BuscarPartesUseCase buscarPartes;
    private final BuscarAdvogadosUseCase buscarAdvogados;
    private final BuscarMagistradosUseCase buscarMagistrados;
    private final BuscarDocumentosUseCase buscarDocumentos;
    private final BuscarMovimentacoesUseCase buscarMovimentacoes;
    private final BuscarDecisaoUseCase buscarDecisao;
    private final BuscarDocumentoTextoUseCase buscarDocumentoTexto;
    private final BuscarContextoUseCase buscarContexto;
    private final BuscarDocumentosComTextoUseCase buscarDocumentosComTexto;
    private final PesquisarProcessosUseCase pesquisarProcessos;

    public ProcessoController(
            BuscarProcessoUseCase buscarProcesso,
            BuscarPartesUseCase buscarPartes,
            BuscarAdvogadosUseCase buscarAdvogados,
            BuscarMagistradosUseCase buscarMagistrados,
            BuscarDocumentosUseCase buscarDocumentos,
            BuscarMovimentacoesUseCase buscarMovimentacoes,
            BuscarDecisaoUseCase buscarDecisao,
            BuscarDocumentoTextoUseCase buscarDocumentoTexto,
            BuscarContextoUseCase buscarContexto,
            BuscarDocumentosComTextoUseCase buscarDocumentosComTexto,
            PesquisarProcessosUseCase pesquisarProcessos) {
        this.buscarProcesso = buscarProcesso;
        this.buscarPartes = buscarPartes;
        this.buscarAdvogados = buscarAdvogados;
        this.buscarMagistrados = buscarMagistrados;
        this.buscarDocumentos = buscarDocumentos;
        this.buscarMovimentacoes = buscarMovimentacoes;
        this.buscarDecisao = buscarDecisao;
        this.buscarDocumentoTexto = buscarDocumentoTexto;
        this.buscarContexto = buscarContexto;
        this.buscarDocumentosComTexto = buscarDocumentosComTexto;
        this.pesquisarProcessos = pesquisarProcessos;
    }

    @Operation(summary = "Buscar processo", description = "Retorna os dados principais de um processo pelo número CNJ")
    @GetMapping("/{numero}")
    public ResponseEntity<ProcessoResponse> buscarProcesso(
            @Parameter(description = "Número do processo (formato CNJ: NNNNNNN-DD.AAAA.J.TT.OOOO)")
            @PathVariable String numero) {
        log.info("GET /api/processos/{}", numero);
        long inicio = System.currentTimeMillis();

        return buscarProcesso.buscar(numero)
                .map(p -> {
                    log.info("Processo {} encontrado em {}ms", numero, System.currentTimeMillis() - inicio);
                    return ResponseEntity.ok(ProcessoResponse.encontrado(p));
                })
                .orElseGet(() -> {
                    log.info("Processo {} não encontrado em {}ms", numero, System.currentTimeMillis() - inicio);
                    return ResponseEntity.status(404).body(ProcessoResponse.naoEncontrado(numero));
                });
    }

    @Operation(summary = "Buscar partes", description = "Lista autores, réus e demais partes do processo")
    @GetMapping("/{numero}/partes")
    public ResponseEntity<ParteResponse> buscarPartes(@PathVariable String numero) {
        log.info("GET /api/processos/{}/partes", numero);
        assertProcessoExiste(numero);
        List<br.jus.tjsc.ai.process.domain.model.Parte> partes = buscarPartes.buscar(numero);
        return ResponseEntity.ok(ParteResponse.of(partes, numero));
    }

    @Operation(summary = "Buscar advogados", description = "Lista advogados identificados nos documentos do processo (OAB)")
    @GetMapping("/{numero}/advogados")
    public ResponseEntity<AdvogadoResponse> buscarAdvogados(@PathVariable String numero) {
        log.info("GET /api/processos/{}/advogados", numero);
        assertProcessoExiste(numero);
        return ResponseEntity.ok(AdvogadoResponse.of(buscarAdvogados.buscar(numero), numero));
    }

    @Operation(summary = "Buscar magistrado", description = "Retorna o magistrado responsável pelo processo")
    @GetMapping("/{numero}/magistrados")
    public ResponseEntity<MagistradoResponse> buscarMagistrados(@PathVariable String numero) {
        log.info("GET /api/processos/{}/magistrados", numero);
        assertProcessoExiste(numero);
        return ResponseEntity.ok(MagistradoResponse.of(buscarMagistrados.buscar(numero), numero));
    }

    @Operation(summary = "Buscar documentos", description = "Lista documentos do processo com paginação (sem texto integral)")
    @GetMapping("/{numero}/documentos")
    public ResponseEntity<PagedResponse<DocumentoDto>> buscarDocumentos(
            @PathVariable String numero,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("GET /api/processos/{}/documentos?page={}&size={}", numero, page, size);
        assertProcessoExiste(numero);

        List<Documento> docs = buscarDocumentos.buscar(numero, page, size);
        long total = buscarDocumentos.contar(numero);

        if (total == 0) {
            return ResponseEntity.ok(PagedResponse.empty("Nenhum documento foi encontrado para este processo."));
        }

        List<DocumentoDto> dtos = docs.stream().map(DocumentoDto::from).toList();
        List<Evidence> evs = docs.stream()
                .map(d -> Evidence.of("DOCUMENTO", d.id(), d.tipo(), numero))
                .toList();

        return ResponseEntity.ok(PagedResponse.of(dtos, page, size, total, evs));
    }

    @Operation(summary = "Buscar movimentações", description = "Lista movimentações processuais ordenadas por data decrescente, com paginação")
    @GetMapping("/{numero}/movimentacoes")
    public ResponseEntity<PagedResponse<MovimentacaoDto>> buscarMovimentacoes(
            @PathVariable String numero,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("GET /api/processos/{}/movimentacoes?page={}&size={}", numero, page, size);
        assertProcessoExiste(numero);

        List<Movimentacao> movs = buscarMovimentacoes.buscar(numero, page, size);
        long total = buscarMovimentacoes.contar(numero);

        if (total == 0) {
            return ResponseEntity.ok(PagedResponse.empty("Nenhuma movimentação foi encontrada para este processo."));
        }

        List<MovimentacaoDto> dtos = movs.stream().map(MovimentacaoDto::from).toList();
        List<Evidence> evs = movs.stream()
                .map(m -> Evidence.of("MOVIMENTACAO", m.id(), m.descricao(), numero))
                .toList();

        return ResponseEntity.ok(PagedResponse.of(dtos, page, size, total, evs));
    }

    @Operation(summary = "Buscar decisão", description = "Retorna a decisão/sentença principal do processo")
    @GetMapping("/{numero}/decisao")
    public ResponseEntity<DecisaoResponse> buscarDecisao(@PathVariable String numero) {
        log.info("GET /api/processos/{}/decisao", numero);
        assertProcessoExiste(numero);
        return buscarDecisao.buscar(numero)
                .map(d -> ResponseEntity.ok(DecisaoResponse.encontrada(d, numero)))
                .orElseGet(() -> ResponseEntity.ok(DecisaoResponse.naoEncontrada()));
    }

    @Operation(summary = "Buscar sentença com texto", description = "Retorna a sentença principal com o texto integral — usado pelo MCP")
    @GetMapping("/{numero}/sentenca")
    public ResponseEntity<DocumentoTextoResponse> buscarSentenca(@PathVariable String numero) {
        log.info("GET /v1/api/processos/{}/sentenca", numero);
        assertProcessoExiste(numero);
        return buscarDocumentoTexto.buscarSentenca(numero)
                .map(d -> ResponseEntity.ok(DocumentoTextoResponse.encontrado(d, numero)))
                .orElseGet(() -> ResponseEntity.ok(
                        DocumentoTextoResponse.naoEncontrado("Nenhuma sentença foi encontrada para este processo.")));
    }

    @Operation(summary = "Buscar petição inicial com texto", description = "Retorna a petição inicial com o texto integral — usado pelo MCP")
    @GetMapping("/{numero}/peticao")
    public ResponseEntity<DocumentoTextoResponse> buscarPeticaoInicial(@PathVariable String numero) {
        log.info("GET /v1/api/processos/{}/peticao", numero);
        assertProcessoExiste(numero);
        return buscarDocumentoTexto.buscarPeticaoInicial(numero)
                .map(d -> ResponseEntity.ok(DocumentoTextoResponse.encontrado(d, numero)))
                .orElseGet(() -> ResponseEntity.ok(
                        DocumentoTextoResponse.naoEncontrado("Nenhuma petição inicial foi encontrada para este processo.")));
    }

    @Operation(summary = "Buscar documento por ID com texto", description = "Retorna um documento específico com texto integral")
    @GetMapping("/{numero}/documentos/{documentoId}")
    public ResponseEntity<DocumentoTextoResponse> buscarDocumentoPorId(
            @PathVariable String numero,
            @PathVariable Long documentoId) {
        log.info("GET /v1/api/processos/{}/documentos/{}", numero, documentoId);
        assertProcessoExiste(numero);
        return buscarDocumentoTexto.buscarPorId(numero, documentoId)
                .map(d -> ResponseEntity.ok(DocumentoTextoResponse.encontrado(d, numero)))
                .orElseGet(() -> ResponseEntity.status(404).body(
                        DocumentoTextoResponse.naoEncontrado("Documento não encontrado neste processo.")));
    }

    @Operation(
        summary = "Contexto completo do processo",
        description = "Retorna tudo em uma única chamada: processo, partes, advogados, magistrado, " +
                      "movimentações, sentença com texto e petição inicial com texto. " +
                      "Endpoint otimizado para consumo pelo MCP Server.")
    @GetMapping("/{numero}/contexto")
    public ResponseEntity<ContextoResponse> buscarContexto(@PathVariable String numero) {
        log.info("GET /v1/api/processos/{}/contexto", numero);
        return ResponseEntity.ok(ContextoResponse.from(buscarContexto.buscar(numero)));
    }

    @Operation(
        summary = "Buscar documentos com texto por tipo",
        description = "Retorna documentos com texto integral filtrados por tipo (ex: Contestação, Despacho, Ata de Audiência). " +
                      "Omitir 'tipo' retorna todos os documentos com texto.")
    @GetMapping("/{numero}/documentos/texto")
    public ResponseEntity<PagedResponse<DocumentoTextoDto>> buscarDocumentosComTexto(
            @PathVariable String numero,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("GET /v1/api/processos/{}/documentos/texto?tipo={}&page={}&size={}", numero, tipo, page, size);
        assertProcessoExiste(numero);

        List<DocumentoTexto> docs = buscarDocumentosComTexto.buscar(numero, tipo, page, size);
        long total = buscarDocumentosComTexto.contar(numero, tipo);

        if (total == 0) {
            return ResponseEntity.ok(PagedResponse.empty("Nenhum documento encontrado para este processo" +
                    (tipo != null ? " com tipo '" + tipo + "'" : "") + "."));
        }

        List<DocumentoTextoDto> dtos = docs.stream().map(DocumentoTextoDto::from).toList();
        List<Evidence> evs = docs.stream()
                .map(d -> Evidence.of("DOCUMENTO", d.id(), d.tipo(), numero))
                .toList();
        return ResponseEntity.ok(PagedResponse.of(dtos, page, size, total, evs));
    }

    @Operation(
        summary = "Pesquisar processos",
        description = "Busca processos por nome de parte, classe, comarca e/ou situação. " +
                      "Todos os filtros são opcionais e combinados com AND. " +
                      "Suporta busca parcial por nome de parte e situação.")
    @GetMapping("/pesquisar")
    public ResponseEntity<PagedResponse<ProcessoDto>> pesquisar(
            @RequestParam(required = false) @Parameter(description = "Nome ou parte do nome da parte (autor/réu)") String parte,
            @RequestParam(required = false) @Parameter(description = "Classe processual exata (ex: Procedimento Comum Cível)") String classe,
            @RequestParam(required = false) @Parameter(description = "Comarca exata (ex: Florianópolis)") String comarca,
            @RequestParam(required = false) @Parameter(description = "Situação (busca parcial, ex: julgado, andamento)") String situacao,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("GET /v1/api/processos/pesquisar?parte={}&classe={}&comarca={}&situacao={}", parte, classe, comarca, situacao);

        List<Processo> processos = pesquisarProcessos.pesquisar(parte, classe, comarca, situacao, page, size);
        long total = pesquisarProcessos.contar(parte, classe, comarca, situacao);

        if (total == 0) {
            return ResponseEntity.ok(PagedResponse.empty("Nenhum processo encontrado para os filtros informados."));
        }

        List<ProcessoDto> dtos = processos.stream().map(ProcessoDto::from).toList();
        List<Evidence> evs = processos.stream()
                .map(p -> Evidence.of("PROCESSO", p.id(), p.numero(), p.numero()))
                .toList();
        return ResponseEntity.ok(PagedResponse.of(dtos, page, size, total, evs));
    }

    private void assertProcessoExiste(String numero) {
        buscarProcesso.buscar(numero)
                .orElseThrow(() -> new br.jus.tjsc.ai.process.domain.exception.ProcessoNaoEncontradoException(numero));
    }
}
