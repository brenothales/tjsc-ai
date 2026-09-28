package br.jus.tjsc.ai.agent.guardrail.tool.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqlOperationToolPolicyTest {

    private SqlOperationToolPolicy policy;
    private ToolDefinition sqlTool;
    private ToolDefinition otherTool;

    @BeforeEach
    void setUp() {
        policy = new SqlOperationToolPolicy(new ObjectMapper());

        sqlTool = mock(ToolDefinition.class);
        when(sqlTool.name()).thenReturn("executar_sql");

        otherTool = mock(ToolDefinition.class);
        when(otherTool.name()).thenReturn("buscar_processo");
    }

    @Test
    void shouldSkipNonSqlTools() {
        String args = "{\"sql\": \"DROP TABLE processos\"}";
        assertThat(policy.evaluate(otherTool, args)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sql\": \"SELECT * FROM processos\"}",
            "{\"sql\": \"SELECT COUNT(*) FROM processos WHERE situacao = 'JULGADO'\"}",
            "{\"sql\": \"SELECT magistrado, COUNT(*) FROM processos GROUP BY magistrado ORDER BY COUNT(*) DESC LIMIT 10\"}"
    })
    void shouldAllowSelectQueries(String args) {
        assertThat(policy.evaluate(sqlTool, args)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sql\": \"DROP TABLE processos\"}",
            "{\"sql\": \"INSERT INTO processos VALUES (1, 'test')\"}",
            "{\"sql\": \"UPDATE processos SET situacao = 'CANCELADO'\"}",
            "{\"sql\": \"DELETE FROM processos\"}",
            "{\"sql\": \"CREATE TABLE hack (id INT)\"}",
            "{\"sql\": \"ALTER TABLE processos ADD COLUMN hack TEXT\"}",
            "{\"sql\": \"TRUNCATE TABLE processos\"}",
            "{\"sql\": \"ATTACH DATABASE '/etc/passwd' AS hack\"}",
            "{\"sql\": \"PRAGMA table_info(processos)\"}",
            "{\"sql\": \"SELECT 1; DROP TABLE processos\"}"
    })
    void shouldBlockForbiddenOperations(String args) {
        Optional<GuardrailViolation> result = policy.evaluate(sqlTool, args);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(GuardrailViolation.ViolationType.BLOCKED_TOOL_OPERATION);
    }

    @Test
    void shouldBlockMultipleStatements() {
        String args = "{\"sql\": \"SELECT 1; DROP TABLE processos\"}";
        Optional<GuardrailViolation> result = policy.evaluate(sqlTool, args);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(GuardrailViolation.ViolationType.BLOCKED_TOOL_OPERATION);
    }

    @Test
    void shouldHandleInvalidJson() {
        assertThat(policy.evaluate(sqlTool, "not json")).isEmpty();
    }

    @Test
    void shouldHandleMissingSqlField() {
        assertThat(policy.evaluate(sqlTool, "{\"query\": \"SELECT 1\"}")).isEmpty();
    }
}
