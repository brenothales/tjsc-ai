package br.jus.tjsc.ai.agent.guardrail.tool;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Wraps a ToolCallback and validates arguments against a list of ToolPolicy instances
 * before delegating to the real implementation. Properly forwards ToolContext so MCP
 * metadata converters (ToolContextToMcpMetaConverter) receive the original context.
 */
public class GuardedToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(GuardedToolCallback.class);

    private final ToolCallback delegate;
    private final List<ToolPolicy> policies;

    public GuardedToolCallback(ToolCallback delegate, List<ToolPolicy> policies) {
        this.delegate = delegate;
        this.policies = policies;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolArguments) {
        return call(toolArguments, null);
    }

    @Override
    public String call(String toolArguments, ToolContext toolContext) {
        ToolDefinition definition = delegate.getToolDefinition();

        Optional<GuardrailViolation> violation = evaluate(definition, toolArguments);
        if (violation.isPresent()) {
            GuardrailViolation v = violation.get();
            log.warn("[ToolGuardrail] Blocked — tool={} policy={} type={}",
                    definition.name(), v.policyName(), v.type());
            return v.userFacingMessage();
        }

        return delegate.call(toolArguments, toolContext);
    }

    private Optional<GuardrailViolation> evaluate(ToolDefinition definition, String toolArguments) {
        return policies.stream()
                .map(p -> p.evaluate(definition, toolArguments))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
