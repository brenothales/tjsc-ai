package br.jus.tjsc.ai.process.config;

import br.jus.tjsc.ai.process.domain.exception.NumeroProcessoInvalidoException;
import br.jus.tjsc.ai.process.domain.exception.ProcessoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProcessoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(
            ProcessoNaoEncontradoException ex, HttpServletRequest req) {
        log.info("Processo não encontrado: {}", ex.getNumeroProcesso());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(HttpStatus.NOT_FOUND, "PROCESS_NOT_FOUND", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(NumeroProcessoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> handleInvalido(
            NumeroProcessoInvalidoException ex, HttpServletRequest req) {
        log.warn("Número de processo inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, "INVALID_PROCESS_NUMBER", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {
        log.warn("Parâmetro inválido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest req) {
        log.debug("Recurso estático não encontrado: {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(HttpStatus.NOT_FOUND, "NOT_FOUND",
                        "Recurso não encontrado: " + req.getRequestURI(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Erro interno: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                        "Erro interno do servidor.", req.getRequestURI()));
    }

    private Map<String, Object> errorBody(HttpStatus status, String code, String message, String path) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "code", code,
                "message", message,
                "path", path
        );
    }
}
