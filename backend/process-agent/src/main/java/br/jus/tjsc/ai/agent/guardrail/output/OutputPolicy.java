package br.jus.tjsc.ai.agent.guardrail.output;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;

import java.util.Optional;

public interface OutputPolicy {
    Optional<GuardrailViolation> evaluate(String outputText);
    String getName();
}
