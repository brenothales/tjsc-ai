package br.jus.tjsc.ai.agent.guardrail.tool.policy;

import br.jus.tjsc.ai.agent.guardrail.GuardrailViolation;
import br.jus.tjsc.ai.agent.guardrail.tool.ToolPolicy;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class SqlOperationToolPolicy implements ToolPolicy {

    private static final Logger log = LoggerFactory.getLogger(SqlOperationToolPolicy.class);

    private static final String SQL_TOOL_NAME = "executar_sql";

    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("\\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|REPLACE)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(ATTACH|DETACH|PRAGMA|VACUUM|REINDEX|ANALYZE)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(";.+", Pattern.DOTALL)
    );

    private static final String BLOCKED_RESPONSE =
            "Operação SQL não permitida. Apenas consultas SELECT são aceitas.";

    private final ObjectMapper objectMapper;

    public SqlOperationToolPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<GuardrailViolation> evaluate(ToolDefinition definition, String toolArguments) {
        if (!SQL_TOOL_NAME.equals(definition.name())) {
            return Optional.empty();
        }

        String sql = extractSql(toolArguments);
        if (sql == null || sql.isBlank()) {
            return Optional.empty();
        }

        String normalizedSql = sql.strip();

        if (!normalizedSql.toUpperCase().startsWith("SELECT")) {
            return Optional.of(new GuardrailViolation(
                    GuardrailViolation.ViolationType.BLOCKED_TOOL_OPERATION,
                    getName(),
                    "SQL must start with SELECT, got: " + normalizedSql.substring(0, Math.min(50, normalizedSql.length())),
                    BLOCKED_RESPONSE
            ));
        }

        for (Pattern forbidden : FORBIDDEN_PATTERNS) {
            if (forbidden.matcher(normalizedSql).find()) {
                return Optional.of(new GuardrailViolation(
                        GuardrailViolation.ViolationType.BLOCKED_TOOL_OPERATION,
                        getName(),
                        "Forbidden SQL pattern detected: " + forbidden.pattern(),
                        BLOCKED_RESPONSE
                ));
            }
        }

        return Optional.empty();
    }

    private String extractSql(String toolArguments) {
        try {
            JsonNode node = objectMapper.readTree(toolArguments);
            JsonNode sqlNode = node.get("sql");
            return sqlNode != null ? sqlNode.asText() : null;
        } catch (Exception e) {
            log.warn("[SqlOperationToolPolicy] Failed to parse tool arguments: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getName() {
        return "SqlOperationToolPolicy";
    }
}
