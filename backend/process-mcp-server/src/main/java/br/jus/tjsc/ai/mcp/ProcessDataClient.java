package br.jus.tjsc.ai.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.function.Supplier;

@Component
public class ProcessDataClient {

    private static final Logger log = LoggerFactory.getLogger(ProcessDataClient.class);

    private final ProcessDataApi api;
    private final SqlDataApi sqlApi;

    ProcessDataClient(ProcessDataApi api, SqlDataApi sqlApi) {
        this.api = api;
        this.sqlApi = sqlApi;
    }

    public String buscarProcesso(String numero) {
        return call(() -> api.buscarProcesso(numero));
    }

    public String buscarContexto(String numero) {
        return call(() -> api.buscarContexto(numero));
    }

    public String buscarPartes(String numero) {
        return call(() -> api.buscarPartes(numero));
    }

    public String buscarAdvogados(String numero) {
        return call(() -> api.buscarAdvogados(numero));
    }

    public String buscarMagistrados(String numero) {
        return call(() -> api.buscarMagistrados(numero));
    }

    public String buscarMovimentacoes(String numero, int page, int size) {
        return call(() -> api.buscarMovimentacoes(numero, page, size));
    }

    public String buscarDocumentos(String numero, int page, int size) {
        return call(() -> api.buscarDocumentos(numero, page, size));
    }

    public String buscarDocumentoPorId(String numero, Long documentoId) {
        return call(() -> api.buscarDocumentoPorId(numero, documentoId));
    }

    public String buscarDocumentosComTexto(String numero, String tipo, int page, int size) {
        return call(() -> api.buscarDocumentosComTexto(numero, tipo, page, size));
    }

    public String buscarSentenca(String numero) {
        return call(() -> api.buscarSentenca(numero));
    }

    public String buscarPeticaoInicial(String numero) {
        return call(() -> api.buscarPeticaoInicial(numero));
    }

    public String buscarDecisao(String numero) {
        return call(() -> api.buscarDecisao(numero));
    }

    public String pesquisarProcessos(String parte, String classe, String comarca, String situacao, int page, int size) {
        return call(() -> api.pesquisarProcessos(parte, classe, comarca, situacao, page, size));
    }

    public String executarSql(String sql) {
        return call(() -> sqlApi.executarSql(java.util.Map.of("sql", sql)));
    }

    private String call(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException e) {
            log.warn("HTTP {} do data service: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return "{\"found\":false,\"message\":\"Recurso não encontrado.\"}";
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return "{\"found\":false,\"message\":\"Número de processo inválido ou parâmetro incorreto.\"}";
            }
            return "{\"found\":false,\"message\":\"Erro HTTP " + e.getStatusCode() + " ao consultar o serviço de dados.\"}";
        } catch (Exception e) {
            log.error("Erro ao chamar process-data-service: {}", e.getMessage());
            return "{\"found\":false,\"message\":\"Serviço de dados indisponível.\"}";
        }
    }
}
