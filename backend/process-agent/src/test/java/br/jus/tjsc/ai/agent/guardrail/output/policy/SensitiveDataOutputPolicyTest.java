package br.jus.tjsc.ai.agent.guardrail.output.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailProperties;
import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataOutputPolicyTest {

    private SensitiveDataOutputPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SensitiveDataOutputPolicy(new GuardrailProperties());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A chave da API é sk-proj-ABCDEFGHIJKLMNOPQRSTUVwxyz12345",
            "sk-ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwx",
            "A variável OPENAI_API_KEY está configurada",
            "mongodb://user:password@localhost:27017/db",
            "password: minha_senha_secreta",
            "secret: valor_secreto_aqui"
    })
    void shouldBlockSensitiveOutput(String text) {
        Optional<GuardrailViolation> result = policy.evaluate(text);

        assertThat(result).isPresent();
        assertThat(result.get().type()).isEqualTo(GuardrailViolation.ViolationType.SENSITIVE_OUTPUT);
        assertThat(result.get().policyName()).isEqualTo("SensitiveDataOutputPolicy");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "O processo 1234567-89.2023.8.24.0001 está em andamento.",
            "A magistrada julgou 42 processos este mês.",
            "Movimentação registrada em 15/03/2024."
    })
    void shouldAllowNormalOutput(String text) {
        assertThat(policy.evaluate(text)).isEmpty();
    }

    @Test
    void shouldReturnSanitizedUserMessage() {
        String sensitiveOutput = "mongodb://admin:supersecret@prod-db:27017/judicial";
        Optional<GuardrailViolation> result = policy.evaluate(sensitiveOutput);

        assertThat(result).isPresent();
        assertThat(result.get().userFacingMessage()).isNotBlank();
        assertThat(result.get().userFacingMessage()).doesNotContain("supersecret");
    }
}
