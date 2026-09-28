package br.jus.tjsc.ai.process.domain.model;

import br.jus.tjsc.ai.process.domain.exception.NumeroProcessoInvalidoException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Normaliza e valida o número do processo no formato CNJ:
 * NNNNNNN-DD.AAAA.J.TT.OOOO (ex: 0000001-02.2019.8.99.9018)
 *
 * Aceita:
 *   - Formato já normalizado: 0000001-02.2019.8.99.9018
 *   - 20 dígitos contíguos:   00000010220198999018
 *   - Com prefixo textual:    "Processo 0000001-02.2019.8.99.9018"
 */
@Component
public class NumeroProcessoNormalizer {

    private static final Pattern FORMATO_CNJ =
            Pattern.compile("^\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}$");

    private static final Pattern FORMATO_DIGITOS =
            Pattern.compile("^\\d{20}$");

    public String normalizar(String input) {
        if (input == null || input.isBlank()) {
            throw new NumeroProcessoInvalidoException("Número do processo não informado.");
        }

        String sanitizado = sanitizar(input);

        if (FORMATO_CNJ.matcher(sanitizado).matches()) {
            return sanitizado;
        }

        if (FORMATO_DIGITOS.matcher(sanitizado).matches()) {
            return formatarDe20Digitos(sanitizado);
        }

        // Tenta extrair somente dígitos e reconstruir se tiver exatamente 20
        String soDigitos = sanitizado.replaceAll("[^\\d]", "");
        if (soDigitos.length() == 20) {
            String candidato = formatarDe20Digitos(soDigitos);
            if (FORMATO_CNJ.matcher(candidato).matches()) {
                return candidato;
            }
        }

        throw new NumeroProcessoInvalidoException(
                "Número de processo inválido: '" + input + "'. " +
                "Formato esperado: NNNNNNN-DD.AAAA.J.TT.OOOO (ex: 0000001-02.2019.8.99.9018).");
    }

    private String sanitizar(String input) {
        return input
                .replaceAll("(?i)^processo\\s+", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private String formatarDe20Digitos(String digits) {
        // NNNNNNN(7) DD(2) AAAA(4) J(1) TT(2) OOOO(4)
        String n = digits.substring(0, 7);
        String d = digits.substring(7, 9);
        String a = digits.substring(9, 13);
        String j = digits.substring(13, 14);
        String t = digits.substring(14, 16);
        String o = digits.substring(16, 20);
        return n + "-" + d + "." + a + "." + j + "." + t + "." + o;
    }
}
