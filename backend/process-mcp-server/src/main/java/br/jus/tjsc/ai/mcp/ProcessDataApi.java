package br.jus.tjsc.ai.mcp;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/v1/processos")
public interface ProcessDataApi {

    @GetExchange("/{numero}")
    String buscarProcesso(@PathVariable String numero);

    @GetExchange("/{numero}/contexto")
    String buscarContexto(@PathVariable String numero);

    @GetExchange("/{numero}/partes")
    String buscarPartes(@PathVariable String numero);

    @GetExchange("/{numero}/advogados")
    String buscarAdvogados(@PathVariable String numero);

    @GetExchange("/{numero}/magistrados")
    String buscarMagistrados(@PathVariable String numero);

    @GetExchange("/{numero}/movimentacoes")
    String buscarMovimentacoes(
            @PathVariable String numero,
            @RequestParam int page,
            @RequestParam int size);

    @GetExchange("/{numero}/documentos")
    String buscarDocumentos(
            @PathVariable String numero,
            @RequestParam int page,
            @RequestParam int size);

    @GetExchange("/{numero}/documentos/{documentoId}")
    String buscarDocumentoPorId(
            @PathVariable String numero,
            @PathVariable Long documentoId);

    @GetExchange("/{numero}/documentos/texto")
    String buscarDocumentosComTexto(
            @PathVariable String numero,
            @RequestParam(required = false) String tipo,
            @RequestParam int page,
            @RequestParam int size);

    @GetExchange("/{numero}/sentenca")
    String buscarSentenca(@PathVariable String numero);

    @GetExchange("/{numero}/peticao")
    String buscarPeticaoInicial(@PathVariable String numero);

    @GetExchange("/{numero}/decisao")
    String buscarDecisao(@PathVariable String numero);

    @GetExchange("/pesquisar")
    String pesquisarProcessos(
            @RequestParam(required = false) String parte,
            @RequestParam(required = false) String classe,
            @RequestParam(required = false) String comarca,
            @RequestParam(required = false) String situacao,
            @RequestParam int page,
            @RequestParam int size);

}
