package br.jus.tjsc.ai.process.domain;

import br.jus.tjsc.ai.process.domain.exception.NumeroProcessoInvalidoException;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumeroProcessoNormalizerTest {

    private NumeroProcessoNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new NumeroProcessoNormalizer();
    }

    @Test
    void deveAceitarFormatoJaNormalizado() {
        String resultado = normalizer.normalizar("0000001-02.2019.8.99.9018");
        assertThat(resultado).isEqualTo("0000001-02.2019.8.99.9018");
    }

    @Test
    void deveNormalizar20Digitos() {
        String resultado = normalizer.normalizar("00000010220198999018");
        assertThat(resultado).isEqualTo("0000001-02.2019.8.99.9018");
    }

    @Test
    void deveRemoverPrefixoProcesso() {
        String resultado = normalizer.normalizar("Processo 0000001-02.2019.8.99.9018");
        assertThat(resultado).isEqualTo("0000001-02.2019.8.99.9018");
    }

    @Test
    void deveRemoverPrefixoProcessoCaseInsensitive() {
        String resultado = normalizer.normalizar("PROCESSO 0000001-02.2019.8.99.9018");
        assertThat(resultado).isEqualTo("0000001-02.2019.8.99.9018");
    }

    @Test
    void deveRemoverEspacos() {
        String resultado = normalizer.normalizar("  0000001-02.2019.8.99.9018  ");
        assertThat(resultado).isEqualTo("0000001-02.2019.8.99.9018");
    }

    // Entradas sem 20 dígitos CNJ extraíveis: devem ser rejeitadas
    @ParameterizedTest(name = "entrada inválida [{0}] deve ser rejeitada")
    @ValueSource(strings = {
            "' OR '1'='1",
            "1; DROP TABLE processo;",
            "\" OR \"1\"=\"1",
            "SELECT * FROM processo",
            "UNION SELECT * FROM processo",
            "12345",
            "abc-de.fghi.j.kl.mnop"
    })
    void deveRejeitarEntradaSemNumeroCnjValido(String entrada) {
        assertThatThrownBy(() -> normalizer.normalizar(entrada))
                .isInstanceOf(NumeroProcessoInvalidoException.class);
    }

    /**
     * Entradas com 20 dígitos CNJ extraíveis (ex: "5001234-56.2024.8.24.0000'; DROP TABLE")
     * são normalizadas para o número CNJ — os caracteres extras são descartados.
     * A proteção real contra SQL injection é o uso de queries parametrizadas no repository,
     * não a rejeição da entrada pelo normalizer.
     */
    @ParameterizedTest(name = "entrada com sufixo malicioso [{0}] deve ser normalizada como CNJ")
    @ValueSource(strings = {
            "5001234-56.2024.8.24.0000'; DROP TABLE processo; --",
            "0000001-02.2019.8.99.9018; DELETE FROM processo"
    })
    void deveNormalizarEntradaComSufixoMaliciosoQuandoContem20DigitosValidos(String entrada) {
        // O normalizer extrai os 20 dígitos CNJ válidos e ignora o resto
        String resultado = normalizer.normalizar(entrada);
        assertThat(resultado).matches("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");
    }

    @Test
    void deveRejeitarNulo() {
        assertThatThrownBy(() -> normalizer.normalizar(null))
                .isInstanceOf(NumeroProcessoInvalidoException.class)
                .hasMessageContaining("não informado");
    }

    @Test
    void deveRejeitarVazio() {
        assertThatThrownBy(() -> normalizer.normalizar("   "))
                .isInstanceOf(NumeroProcessoInvalidoException.class);
    }

    @Test
    void deveRejeitarFormatoInvalido() {
        assertThatThrownBy(() -> normalizer.normalizar("12345"))
                .isInstanceOf(NumeroProcessoInvalidoException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void resultadoNormalizadoSempreMatchFormatoCnj() {
        String resultado = normalizer.normalizar("00000010220198999018");
        assertThat(resultado).matches("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");
    }
}
