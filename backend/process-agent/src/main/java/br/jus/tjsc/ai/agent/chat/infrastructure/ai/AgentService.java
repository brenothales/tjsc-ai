package br.jus.tjsc.ai.agent.chat.infrastructure.ai;

import br.jus.tjsc.ai.agent.chat.infrastructure.ai.advisor.LanguageEnforcementAdvisor;
import br.jus.tjsc.ai.agent.guardrail.input.InputGuardrailAdvisor;
import br.jus.tjsc.ai.agent.guardrail.output.OutputGuardrailAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class AgentService {

    private final ChatClient chatClient;
    private final String     baseSystemPrompt;

    AgentService(ChatClient.Builder builder,
                 ToolCallback[] mcpTools,
                 ChatMemory chatMemory,
                 InputGuardrailAdvisor inputGuardrailAdvisor,
                 OutputGuardrailAdvisor outputGuardrailAdvisor,
                 LanguageEnforcementAdvisor languageEnforcementAdvisor,
                 @Value("classpath:prompts/agent_system.st") Resource systemPrompt) throws IOException {

        this.baseSystemPrompt = systemPrompt.getContentAsString(StandardCharsets.UTF_8);

        this.chatClient = builder
                .defaultTools(mcpTools)
                .defaultAdvisors(
                        inputGuardrailAdvisor,
                        languageEnforcementAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor(),
                        outputGuardrailAdvisor)
                .build();
    }

    public Flux<String> chat(String userMessage, String conversationId,
                             String model, Double temperature, String systemExtra) {
        return buildPrompt(userMessage, conversationId, model, temperature, systemExtra)
                .stream()
                .content();
    }

    public String chatSync(String userMessage, String conversationId,
                           String model, Double temperature, String systemExtra) {
        return buildPrompt(userMessage, conversationId, model, temperature, systemExtra)
                .call()
                .content();
    }

    private ChatClient.ChatClientRequestSpec buildPrompt(String userMessage, String conversationId,
                                                         String model, Double temperature, String systemExtra) {
        String systemText = (systemExtra != null && !systemExtra.isBlank())
                ? baseSystemPrompt + "\n\n## Instruções adicionais do usuário\n"
                  + systemExtra.substring(0, Math.min(systemExtra.length(), 500))
                : baseSystemPrompt;

        var spec = chatClient.prompt()
                .system(systemText)
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId));

        if (model != null || temperature != null) {
            OpenAiChatOptions.Builder opts = OpenAiChatOptions.builder();
            if (model != null && !model.isBlank()) opts.model(model);
            if (temperature != null) opts.temperature(temperature);
            spec = spec.options(opts);
        }

        return spec;
    }
}
