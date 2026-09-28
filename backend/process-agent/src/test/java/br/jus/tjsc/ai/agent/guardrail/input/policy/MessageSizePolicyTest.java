package br.jus.tjsc.ai.agent.guardrail.input.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailProperties;
import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSizePolicyTest {

    private MessageSizePolicy policy;
    private static final int MAX_LENGTH = 2000;

    @BeforeEach
    void setUp() {
        policy = new MessageSizePolicy(new GuardrailProperties());
    }

    @Test
    void shouldAllowMessageAtMaxLength() {
        String message = "a".repeat(MAX_LENGTH);
        assertThat(policy.evaluate(message)).isEmpty();
    }

    @Test
    void shouldBlockMessageExceedingMaxLength() {
        String message = "a".repeat(MAX_LENGTH + 1);
        Optional<GuardrailViolation> result = policy.evaluate(message);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(GuardrailViolation.ViolationType.MESSAGE_TOO_LARGE);
        assertThat(result.get().userFacingMessage()).contains(String.valueOf(MAX_LENGTH));
    }

    @Test
    void shouldAllowShortMessage() {
        assertThat(policy.evaluate("Qual o status do processo?")).isEmpty();
    }

    @Test
    void shouldIncludeActualLengthInInternalMessage() {
        String message = "a".repeat(MAX_LENGTH + 100);
        Optional<GuardrailViolation> result = policy.evaluate(message);

        assertThat(result).isPresent();
        assertThat(result.get().internalMessage())
                .contains(String.valueOf(MAX_LENGTH + 100))
                .contains(String.valueOf(MAX_LENGTH));
    }
}
