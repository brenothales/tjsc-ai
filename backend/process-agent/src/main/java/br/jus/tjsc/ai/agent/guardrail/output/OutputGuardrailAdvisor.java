package br.jus.tjsc.ai.agent.guardrail.output;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scans LLM output for sensitive data or policy violations before returning to the caller.
 * Replaces the response with a safe message when a violation is detected.
 *
 * Order: LOWEST_PRECEDENCE — executes innermost (last on input, first on output).
 */
@Component
public class OutputGuardrailAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(OutputGuardrailAdvisor.class);

    private final List<OutputPolicy> policies;

    public OutputGuardrailAdvisor(List<OutputPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        return sanitize(response);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request).map(this::sanitize);
    }

    private ChatClientResponse sanitize(ChatClientResponse response) {
        String text = extractText(response);
        if (text == null || text.isBlank()) return response;

        Optional<GuardrailViolation> violation = evaluate(text);
        if (violation.isEmpty()) return response;

        GuardrailViolation v = violation.get();
        log.warn("[OutputGuardrail] Sanitized — policy={} type={}", v.policyName(), v.type());

        AssistantMessage safe = new AssistantMessage(v.userFacingMessage());
        ChatResponse sanitizedChatResponse = new ChatResponse(List.of(new Generation(safe)));

        Map<String, Object> ctx = new HashMap<>(response.context());
        ctx.put("guardrail_output_blocked", true);
        ctx.put("guardrail_output_policy", v.policyName());

        return ChatClientResponse.builder()
                .chatResponse(sanitizedChatResponse)
                .context(ctx)
                .build();
    }

    private Optional<GuardrailViolation> evaluate(String text) {
        return policies.stream()
                .map(p -> p.evaluate(text))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private String extractText(ChatClientResponse response) {
        try {
            return response.chatResponse().getResult().getOutput().getText();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return "OutputGuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
