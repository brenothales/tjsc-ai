package br.jus.tjsc.ai.agent.voice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice(assignableTypes = VoiceController.class)
class VoiceControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(VoiceControllerAdvice.class);

    @ExceptionHandler(HttpClientErrorException.class)
    ProblemDetail handleOpenAiError(HttpClientErrorException ex) {
        log.error("[Voice] erro ao chamar OpenAI — status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        var detail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        detail.setTitle("OpenAI Realtime indisponível");
        detail.setDetail("Verifique se a chave OpenAI tem acesso ao Realtime API (gpt-4o-realtime-preview). Detalhe: " + ex.getMessage());
        return detail;
    }

    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail handleNetworkError(ResourceAccessException ex) {
        log.error("[Voice] erro de rede ao chamar OpenAI: {}", ex.getMessage());
        var detail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        detail.setTitle("Erro de conexão com OpenAI");
        detail.setDetail(ex.getMessage());
        return detail;
    }
}
