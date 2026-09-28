package br.jus.tjsc.ai.agent.minuta.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class ColetaDadosService {

    private final ChatClient   chatClient;
    private final ToolCallback[] mcpTools;
    private final Resource     systemTpl;
    private final Resource     userTpl;

    ColetaDadosService(@Qualifier("minutaChatClient") ChatClient chatClient,
                       ToolCallback[] mcpTools,
                       @Value("classpath:prompts/coleta-system.st") Resource systemTpl,
                       @Value("classpath:prompts/coleta-user.st")   Resource userTpl) {
        this.chatClient = chatClient;
        this.mcpTools   = mcpTools;
        this.systemTpl  = systemTpl;
        this.userTpl    = userTpl;
    }

    String coletar(String numero) {
        String system = new PromptTemplate(systemTpl).render();
        String user   = new PromptTemplate(userTpl).render(Map.of("numero", numero));

        return chatClient.prompt()
                .system(system)
                .user(user)
                .tools(mcpTools)
                .call()
                .content();
    }
}
