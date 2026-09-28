package br.jus.tjsc.ai.agent.guardrail.tool;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Optional;

public interface ToolPolicy {
    Optional<GuardrailViolation> evaluate(ToolDefinition definition, String toolArguments);
    String getName();
}
