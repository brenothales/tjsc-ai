package br.jus.tjsc.ai.agent.guardrail.input.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailProperties;
import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import br.jus.tjsc.ai.agent.guardrail.input.InputPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class PromptInjectionPolicy implements InputPolicy {

    private final List<Pattern> patterns;
    private final String failureResponse;

    public PromptInjectionPolicy(GuardrailProperties properties) {
        this.patterns = properties.getInput().getInjectionPatterns().stream()
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList();
        this.failureResponse = properties.getInput().getInjectionResponse();
    }

    @Override
    public Optional<GuardrailViolation> evaluate(String userMessage) {
        return patterns.stream()
                .filter(p -> p.matcher(userMessage).find())
                .findFirst()
                .map(matched -> new GuardrailViolation(
                        GuardrailViolation.ViolationType.PROMPT_INJECTION,
                        getName(),
                        "Prompt injection pattern matched: " + matched.pattern(),
                        failureResponse
                ));
    }

    @Override
    public String getName() {
        return "PromptInjectionPolicy";
    }
}
