package br.jus.tjsc.ai.agent.guardrail;

import br.jus.tjsc.ai.agent.guardrail.tool.GuardedToolCallback;
import br.jus.tjsc.ai.agent.guardrail.tool.ToolPolicy;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GuardrailConfig {

    @Bean
    ToolCallback[] mcpTools(
            SyncMcpToolCallbackProvider mcpToolCallbacks,
            List<ToolPolicy> toolPolicies) {

        return Arrays.stream(mcpToolCallbacks.getToolCallbacks())
                .map(tool -> (ToolCallback) new GuardedToolCallback(tool, toolPolicies))
                .toArray(ToolCallback[]::new);
    }
}
