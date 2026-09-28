package br.jus.tjsc.ai.agent.chat.infrastructure.ai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LanguageEnforcementAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String INSTRUCTION =
            "\n\nRESPONDA SEMPRE EM PORTUGUÊS DO BRASIL, independentemente do idioma da mensagem do usuário.";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(augment(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(augment(request));
    }

    private ChatClientRequest augment(ChatClientRequest request) {
        Prompt augmented = request.prompt().augmentSystemMessage(INSTRUCTION);
        return request.mutate().prompt(augmented).build();
    }

    @Override
    public String getName() { return "LanguageEnforcementAdvisor"; }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 1; }
}
