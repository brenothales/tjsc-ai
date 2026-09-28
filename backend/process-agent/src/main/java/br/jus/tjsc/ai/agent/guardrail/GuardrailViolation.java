package br.jus.tjsc.ai.agent.guardrail;

public record GuardrailViolation(
        ViolationType type,
        String policyName,
        String internalMessage,
        String userFacingMessage
) {
    public enum ViolationType {
        PROMPT_INJECTION,
        MESSAGE_TOO_LARGE,
        OFF_DOMAIN,
        SENSITIVE_OUTPUT,
        BLOCKED_TOOL_OPERATION
    }
}
