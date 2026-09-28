package br.jus.tjsc.ai.process.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/sql")
@Tag(name = "SQL", description = "Consulta SQL genérica somente leitura — uso exclusivo pelo MCP Server")
class SqlController {

    private static final Logger log = LoggerFactory.getLogger(SqlController.class);
    private static final int MAX_ROWS = 100;

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "TRUNCATE", "REPLACE", "ATTACH", "DETACH", "PRAGMA",
            "VACUUM", "REINDEX", "ANALYZE", "USE"
    );

    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", BLOCKED_KEYWORDS) + ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final JdbcTemplate jdbc;

    SqlController(@Qualifier("readOnlyJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Operation(summary = "Executar consulta SQL SELECT")
    @PostMapping
    SqlQueryResponse executar(@RequestBody SqlQueryRequest request) {
        String sql = request.sql().strip();
        log.info("SQL query: {}", sql);

        if (!sql.toUpperCase().startsWith("SELECT")) {
            return SqlQueryResponse.erro("Apenas consultas SELECT são permitidas.");
        }

        sql = sql.stripTrailing();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).stripTrailing();
        }
        if (sql.contains(";")) {
            return SqlQueryResponse.erro("Múltiplos statements não são permitidos.");
        }

        var matcher = KEYWORD_PATTERN.matcher(sql);
        if (matcher.find()) {
            log.warn("SQL bloqueado — keyword proibida '{}': {}", matcher.group(), sql);
            return SqlQueryResponse.erro("Instrução não permitida: contém '" + matcher.group().toUpperCase() + "'.");
        }

        String limitedSql = appendLimitIfAbsent(sql);

        List<Map<String, Object>> rows = jdbc.queryForList(limitedSql);
        return SqlQueryResponse.ok(rows, rows.size());
    }

    private static final Pattern LIMIT_PATTERN =
            Pattern.compile("\\bLIMIT\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private String appendLimitIfAbsent(String sql) {
        var matcher = LIMIT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return sql + " LIMIT " + MAX_ROWS;
        }
        int requested = Integer.parseInt(matcher.group(1));
        if (requested > MAX_ROWS) {
            return LIMIT_PATTERN.matcher(sql).replaceFirst("LIMIT " + MAX_ROWS);
        }
        return sql;
    }

    record SqlQueryRequest(String sql) {}

    record SqlQueryResponse(boolean success, String error, List<Map<String, Object>> rows, int count) {
        static SqlQueryResponse ok(List<Map<String, Object>> rows, int count) {
            return new SqlQueryResponse(true, null, rows, count);
        }
        static SqlQueryResponse erro(String message) {
            return new SqlQueryResponse(false, message, List.of(), 0);
        }
    }
}
