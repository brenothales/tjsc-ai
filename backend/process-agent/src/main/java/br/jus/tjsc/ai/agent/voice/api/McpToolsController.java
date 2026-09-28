package br.jus.tjsc.ai.agent.voice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mcp")
class McpToolsController {

    private static final Logger log = LoggerFactory.getLogger(McpToolsController.class);

    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    McpToolsController(SyncMcpToolCallbackProvider mcpToolCallbackProvider) {
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
    }

    @GetMapping("/tools")
    List<String> tools() {
        var names = Arrays.stream(mcpToolCallbackProvider.getToolCallbacks())
                .map(t -> t.getToolDefinition().name())
                .sorted()
                .toList();
        log.info("[MCP] ferramentas descobertas: {}", names);
        return names;
    }
}
