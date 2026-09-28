package br.jus.tjsc.ai.agent.guardrail;

public class GuardrailException extends RuntimeException {

    private final GuardrailViolation violation;

    public GuardrailException(GuardrailViolation violation) {
        super(violation.internalMessage());
        this.violation = violation;
    }

    public GuardrailViolation getViolation() {
        return violation;
    }
}
