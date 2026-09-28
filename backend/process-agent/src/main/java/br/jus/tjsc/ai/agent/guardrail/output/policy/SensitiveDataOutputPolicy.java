package br.jus.tjsc.ai.agent.guardrail.output.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailProperties;
import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import br.jus.tjsc.ai.agent.guardrail.output.OutputPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SensitiveDataOutputPolicy implements OutputPolicy {

    private final List<Pattern> sensitivePatterns;
    private final String sanitizedResponse;

    public SensitiveDataOutputPolicy(GuardrailProperties properties) {
        this.sensitivePatterns = properties.getOutput().getSensitivePatterns().stream()
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                .toList();
        this.sanitizedResponse = properties.getOutput().getSanitizedResponse();
    }

    @Override
    public Optional<GuardrailViolation> evaluate(String outputText) {
        return sensitivePatterns.stream()
                .filter(p -> p.matcher(outputText).find())
                .findFirst()
                .map(matched -> new GuardrailViolation(
                        GuardrailViolation.ViolationType.SENSITIVE_OUTPUT,
                        getName(),
                        "Sensitive data pattern matched in output: " + matched.pattern(),
                        sanitizedResponse
                ));
    }

    @Override
    public String getName() {
        return "SensitiveDataOutputPolicy";
    }
}
