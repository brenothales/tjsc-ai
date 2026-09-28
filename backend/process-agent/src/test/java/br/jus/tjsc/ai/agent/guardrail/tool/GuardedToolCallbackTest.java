package br.jus.tjsc.ai.agent.guardrail.tool;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GuardedToolCallbackTest {

    @Test
    void shouldDelegateWithContextWhenNoPolicyViolation() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call(any(), any())).thenReturn("result");

        ToolPolicy passingPolicy = mock(ToolPolicy.class);
        when(passingPolicy.evaluate(any(), any())).thenReturn(Optional.empty());

        ToolContext context = new ToolContext(Map.of("key", "value"));
        GuardedToolCallback guarded = new GuardedToolCallback(delegate, List.of(passingPolicy));
        String result = guarded.call("{\"sql\": \"SELECT 1\"}", context);

        assertThat(result).isEqualTo("result");
        verify(delegate).call("{\"sql\": \"SELECT 1\"}", context);
    }

    @Test
    void shouldForwardNullContextToDelegate() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call(any(), eq(null))).thenReturn("result");

        GuardedToolCallback guarded = new GuardedToolCallback(delegate, List.of());
        String result = guarded.call("{\"sql\": \"SELECT 1\"}");

        assertThat(result).isEqualTo("result");
        verify(delegate).call("{\"sql\": \"SELECT 1\"}", null);
    }

    @Test
    void shouldReturnViolationMessageWhenPolicyBlocks() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("executar_sql");
        when(delegate.getToolDefinition()).thenReturn(definition);

        GuardrailViolation violation = new GuardrailViolation(
                GuardrailViolation.ViolationType.BLOCKED_TOOL_OPERATION,
                "TestPolicy", "internal msg", "Operação não permitida.");

        ToolPolicy blockingPolicy = mock(ToolPolicy.class);
        when(blockingPolicy.evaluate(any(), any())).thenReturn(Optional.of(violation));

        GuardedToolCallback guarded = new GuardedToolCallback(delegate, List.of(blockingPolicy));
        String result = guarded.call("{\"sql\": \"DROP TABLE processos\"}", null);

        assertThat(result).isEqualTo("Operação não permitida.");
        verify(delegate, never()).call(any(), any());
    }

    @Test
    void shouldStopAtFirstViolation() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(delegate.getToolDefinition()).thenReturn(definition);

        GuardrailViolation violation = new GuardrailViolation(
                GuardrailViolation.ViolationType.BLOCKED_TOOL_OPERATION,
                "FirstPolicy", "msg", "blocked");

        ToolPolicy firstPolicy = mock(ToolPolicy.class);
        when(firstPolicy.evaluate(any(), any())).thenReturn(Optional.of(violation));

        ToolPolicy secondPolicy = mock(ToolPolicy.class);

        GuardedToolCallback guarded = new GuardedToolCallback(delegate, List.of(firstPolicy, secondPolicy));
        guarded.call("{}", null);

        verify(secondPolicy, never()).evaluate(any(), any());
    }

    @Test
    void shouldExposeDefinitionFromDelegate() {
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(delegate.getToolDefinition()).thenReturn(definition);

        GuardedToolCallback guarded = new GuardedToolCallback(delegate, List.of());

        assertThat(guarded.getToolDefinition()).isSameAs(definition);
    }
}
