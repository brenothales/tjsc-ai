package br.jus.tjsc.ai.agent.voice.application;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class McpToolRegistry {

    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;

    public McpToolRegistry(ToolCallbackProvider toolCallbackProvider, ObjectMapper objectMapper) {
        this.toolCallbackProvider = toolCallbackProvider;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getLiveTools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(this::toLiveTool)
                .toList();
    }

    private Map<String, Object> toLiveTool(ToolCallback callback) {
        ToolDefinition definition = callback.getToolDefinition();
        return Map.of(
                "type", "function",
                "name", definition.name(),
                "description", definition.description(),
                "parameters", parseSchema(definition.inputSchema())
        );
    }

    private JsonNode parseSchema(String schema) {
        try {
            return objectMapper.readTree(schema);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível converter o schema da tool MCP", e);
        }
    }
}
