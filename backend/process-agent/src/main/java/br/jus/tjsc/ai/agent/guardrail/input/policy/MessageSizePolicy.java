package br.jus.tjsc.ai.agent.guardrail.input.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailProperties;
import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import br.jus.tjsc.ai.agent.guardrail.input.InputPolicy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MessageSizePolicy implements InputPolicy {

    private final int maxLength;
    private final String failureResponse;

    public MessageSizePolicy(GuardrailProperties properties) {
        this.maxLength = properties.getInput().getMaxMessageLength();
        this.failureResponse = properties.getInput().getSizeExceededResponse();
    }

    @Override
    public Optional<GuardrailViolation> evaluate(String userMessage) {
        if (userMessage.length() <= maxLength) {
            return Optional.empty();
        }
        return Optional.of(new GuardrailViolation(
                GuardrailViolation.ViolationType.MESSAGE_TOO_LARGE,
                getName(),
                "Message length %d exceeds maximum %d".formatted(userMessage.length(), maxLength),
                failureResponse + " (máximo: %d caracteres)".formatted(maxLength)
        ));
    }

    @Override
    public String getName() {
        return "MessageSizePolicy";
    }
}
