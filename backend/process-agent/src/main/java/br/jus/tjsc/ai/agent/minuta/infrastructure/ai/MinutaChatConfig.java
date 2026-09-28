package br.jus.tjsc.ai.agent.minuta.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MinutaChatConfig {

    @Bean("minutaChatClient")
    ChatClient minutaChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .temperature(0.2))
                .build();
    }
}
