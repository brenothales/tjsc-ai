package br.jus.tjsc.ai.agent.chat.api;

import br.jus.tjsc.ai.agent.chat.application.ConversationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ChatControllerAdvice {

    @ExceptionHandler(ConversationNotFoundException.class)
    ProblemDetail handleNotFound(ConversationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
