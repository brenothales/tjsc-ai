package br.jus.tjsc.ai.agent.minuta.api;

import br.jus.tjsc.ai.agent.minuta.application.MinutaNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "br.jus.tjsc.ai.agent.minuta")
class MinutaControllerAdvice {

    @ExceptionHandler(MinutaNotFoundException.class)
    ProblemDetail handleNotFound(MinutaNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneric(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar minuta");
    }
}
