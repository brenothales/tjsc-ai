package br.jus.tjsc.ai.agent.guardrail.input;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;

import java.util.Optional;

public interface InputPolicy {
    Optional<GuardrailViolation> evaluate(String userMessage);
    String getName();
}
