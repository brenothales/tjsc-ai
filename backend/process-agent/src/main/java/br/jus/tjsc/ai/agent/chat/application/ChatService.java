package br.jus.tjsc.ai.agent.chat.application;

import br.jus.tjsc.ai.agent.chat.api.dto.ChatRequest;
import br.jus.tjsc.ai.agent.chat.api.dto.ChatResponse;
import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<String> stream(ChatRequest request);
    ChatResponse chat(ChatRequest request);
}
