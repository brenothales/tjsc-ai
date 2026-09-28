package br.jus.tjsc.ai.agent.chat.api;

import br.jus.tjsc.ai.agent.chat.api.dto.ChatRequest;
import br.jus.tjsc.ai.agent.chat.api.dto.ChatResponse;
import br.jus.tjsc.ai.agent.chat.application.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
class ChatController {

    private final ChatService chatService;

    ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestBody ChatRequest request) {
        return chatService.stream(request);
    }

    @PostMapping
    ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
