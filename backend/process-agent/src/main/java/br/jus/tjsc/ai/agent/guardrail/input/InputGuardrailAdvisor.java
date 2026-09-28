package br.jus.tjsc.ai.agent.guardrail.input;

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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validates user input before it reaches the LLM.
 * Short-circuits the advisor chain when a policy is violated, returning a rejection
 * response without calling the LLM.
 *
 * Order: HIGHEST_PRECEDENCE — executes outermost (first on input, last on output).
 */
@Component
public class InputGuardrailAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(InputGuardrailAdvisor.class);

    private final List<InputPolicy> policies;

    public InputGuardrailAdvisor(List<InputPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Optional<GuardrailViolation> violation = evaluate(request);
        if (violation.isPresent()) {
            log.warn("[InputGuardrail] Blocked — policy={} type={}",
                    violation.get().policyName(), violation.get().type());
            return buildRejectionResponse(violation.get(), request.context());
        }
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Optional<GuardrailViolation> violation = evaluate(request);
        if (violation.isPresent()) {
            log.warn("[InputGuardrail] Blocked (stream) — policy={} type={}",
                    violation.get().policyName(), violation.get().type());
            return Flux.just(buildRejectionResponse(violation.get(), request.context()));
        }
        return Mono.just(request)
                .publishOn(Schedulers.boundedElastic())
                .flatMapMany(chain::nextStream);
    }

    private Optional<GuardrailViolation> evaluate(ChatClientRequest request) {
        String userText = extractUserText(request);
        if (userText == null || userText.isBlank()) return Optional.empty();

        return policies.stream()
                .map(p -> p.evaluate(userText))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private String extractUserText(ChatClientRequest request) {
        try {
            return request.prompt().getUserMessage().getText();
        } catch (Exception e) {
            return null;
        }
    }

    private ChatClientResponse buildRejectionResponse(GuardrailViolation violation, Map<String, Object> originalContext) {
        AssistantMessage msg = new AssistantMessage(violation.userFacingMessage());
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(msg)));

        Map<String, Object> ctx = new HashMap<>(originalContext);
        ctx.put("guardrail_blocked", true);
        ctx.put("guardrail_violation_type", violation.type().name());
        ctx.put("guardrail_policy", violation.policyName());

        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(ctx)
                .build();
    }

    @Override
    public String getName() {
        return "InputGuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
