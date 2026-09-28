---
type: Architecture Pattern
title: Guardrail em 5 Camadas (agente + banco)
description: Proteção em defesa-em-profundidade — 3 camadas no process-agent (input/tool/output) e 2 camadas independentes no process-data-service (SqlController + JDBC read-only).
tags: [guardrail, segurança, prompt-injection, spring-ai, sqlite, read-only]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/guardrail/input/InputGuardrailAdvisor.java"
    title: "InputGuardrailAdvisor.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/guardrail/output/OutputGuardrailAdvisor.java"
    title: "OutputGuardrailAdvisor.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/guardrail/tool/GuardedToolCallback.java"
    title: "GuardedToolCallback.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/guardrail/tool/policy/SqlOperationToolPolicy.java"
    title: "SqlOperationToolPolicy.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/guardrail/GuardrailProperties.java"
    title: "GuardrailProperties.java"
  - resource: "backend/process-data-service/src/main/java/br/jus/tjsc/ai/process/adapter/in/web/SqlController.java"
    title: "SqlController.java"
  - resource: "backend/process-data-service/src/main/java/br/jus/tjsc/ai/process/config/ReadOnlyDataSourceConfig.java"
    title: "ReadOnlyDataSourceConfig.java"
  - resource: "backend/process-data-service/src/main/resources/application.yml"
    title: "application.yml — hikari connection-init-sql"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Guardrail em 5 Camadas (agente + banco)

Cinco pontos de proteção independentes distribuídos em dois serviços. Um atacante que contorne qualquer camada ainda enfrenta as demais. As camadas 1–3 ficam no `process-agent`; as camadas 4–5 ficam no `process-data-service` e são completamente agnósticas ao LLM.

```
Usuário
  │
  ▼ [1] InputGuardrailAdvisor   — bloqueia prompt injection e SQL literal na mensagem
  │
  ▼ [2] GuardedToolCallback     — bloqueia argumentos inválidos antes de executar ferramenta
  │
  ▼ LLM + MCP tools → process-data-service
  │
  ▼ [3] OutputGuardrailAdvisor  — sanitiza resposta do LLM (API keys, senhas)
  │
  └─────────────────────────────────────────────────────────
  
  Dentro do process-data-service (independente do agente):
  
  ▼ [4] SqlController            — valida SELECT, bloqueia DDL/DML, rejeita multi-statement
  │
  ▼ [5] JDBC READONLY            — SQLiteOpenMode.READONLY + PRAGMA query_only = ON
         (nível de SO — impossível escrever mesmo que tudo acima falhe)
```

## Camada 1 — Input Guardrail

**Classe**: `InputGuardrailAdvisor` (`Ordered.HIGHEST_PRECEDENCE`)  
**Quando**: antes do LLM ver a mensagem  
**Comportamento**: short-circuit — se violar, retorna resposta de rejeição sem chamar LLM nem memória  
**Implementa**: `CallAdvisor` + `StreamAdvisor`

### Políticas

**`PromptInjectionPolicy`**  
Compila padrões regex de `GuardrailProperties.input.injectionPatterns` com `CASE_INSENSITIVE | UNICODE_CASE`. Padrões cobrem:
- Instruções de ignorar system prompt (`ignore todas as instruções`, `ignore tudo acima`)
- Revelar internos (`mostre o system prompt`, `revele o prompt`)
- Jailbreak clássico (`aja como se você fosse`, `pretenda ser`, `DAN`, `sem restrições`)
- SQL direto pelo usuário (`fa\u00e7a um select`, `execute sql`, `SELECT ... FROM`, `DROP TABLE`, `sqlite_master`, `PRAGMA`)

Resposta padrão: `"Sua mensagem foi identificada como potencialmente maliciosa e não pôde ser processada."`

**`MessageSizePolicy`**  
Rejeita mensagens com mais de 2000 caracteres (`guardrail.input.max-message-length`).  
Resposta padrão: `"Sua mensagem é muito longa. Por favor, reformule de forma mais concisa."`

### Flags no contexto de resposta

Quando bloqueia, adiciona ao contexto:
```java
ctx.put("guardrail_blocked", true);
ctx.put("guardrail_violation_type", violation.type().name());  // PROMPT_INJECTION | MESSAGE_TOO_LONG
ctx.put("guardrail_policy", violation.policyName());
```

## Camada 2 — Tool Guardrail

**Classe**: `GuardedToolCallback` (wrapper decorator sobre cada `ToolCallback`)  
**Quando**: durante a execução de qualquer ferramenta MCP, antes de chamar o delegate real  
**Comportamento**: retorna mensagem de rejeição ao LLM (não ao usuário) — o LLM pode tentar outra abordagem

### Política ativa

**`SqlOperationToolPolicy`**  
Aplicada apenas à ferramenta `executar_sql`. Valida o argumento `sql`:
1. Query deve começar com `SELECT`
2. Regex contra keywords: `INSERT UPDATE DELETE DROP CREATE ALTER TRUNCATE REPLACE ATTACH DETACH PRAGMA VACUUM REINDEX ANALYZE`
3. Rejeita multi-statement via `;.+` regex

Resposta ao LLM: `"Operação SQL não permitida. Apenas consultas SELECT são aceitas."`

### Implementação

```java
// GuardedToolCallback.java
@Override
public String call(String toolArguments, ToolContext toolContext) {
    Optional<GuardrailViolation> violation = evaluate(delegate.getToolDefinition(), toolArguments);
    if (violation.isPresent()) {
        return violation.get().userFacingMessage(); // retorna ao LLM, não ao usuário
    }
    return delegate.call(toolArguments, toolContext); // ToolContext propagado para MCP metadata
}
```

## Camada 3 — Output Guardrail

**Classe**: `OutputGuardrailAdvisor` (`Ordered.LOWEST_PRECEDENCE`)  
**Quando**: depois do LLM gerar a resposta, antes de entregar ao usuário  
**Comportamento**: substitui toda a resposta por mensagem segura se detectar violação

### Política ativa

**`SensitiveDataOutputPolicy`**  
Regex contra a resposta completa do LLM:
- `sk-proj-[A-Za-z0-9_-]{20,}` — OpenAI project key
- `sk-[A-Za-z0-9]{48}` — OpenAI legacy key
- `OPENAI_API_KEY` — literal
- `mongodb://[^\s]*:[^\s]*@` — MongoDB URI com credencial
- `(?i)password\s*[:=]\s*\S+` — campo password
- `(?i)secret\s*[:=]\s*\S+` — campo secret

Resposta ao usuário: `"A resposta foi filtrada por conter informações potencialmente sensíveis."`

## Camada 4 — SqlController (process-data-service)

**Classe**: `SqlController` (package-private, não exposto no Swagger público)  
**Serviço**: `process-data-service:8081`  
**Quando**: ao receber `POST /api/v1/sql` — antes de executar qualquer query no SQLite  
**Comportamento**: retorna `SqlQueryResponse(success=false, error="...")` sem tocar no banco

### Validações em sequência

```java
// 1. Obrigatório começar com SELECT
if (!sql.toUpperCase().startsWith("SELECT"))
    return SqlQueryResponse.erro("Apenas consultas SELECT são permitidas.");

// 2. Remove ; trailing — normalização
sql = sql.stripTrailing();
if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).stripTrailing();

// 3. Rejeita multi-statement (ex: SELECT 1; DROP TABLE processo)
if (sql.contains(";"))
    return SqlQueryResponse.erro("Múltiplos statements não são permitidos.");

// 4. Regex contra keywords bloqueadas (word-boundary, case-insensitive)
Set<String> BLOCKED = { "INSERT","UPDATE","DELETE","DROP","CREATE","ALTER",
                        "TRUNCATE","REPLACE","ATTACH","DETACH","PRAGMA",
                        "VACUUM","REINDEX","ANALYZE","USE" }
Pattern.compile("\\b(INSERT|UPDATE|...|USE)\\b", CASE_INSENSITIVE)
// → SqlQueryResponse.erro("Instrução não permitida: contém 'DROP'.")

// 5. Adiciona LIMIT 100 automaticamente se ausente
if (!sql.toUpperCase().contains("LIMIT")) sql += " LIMIT 100";
```

### Por que validar no data-service e não só no agente?

O `SqlController` é um endpoint HTTP — pode ser chamado diretamente, sem passar pelo agente. Um atacante com acesso à rede interna poderia chamar `POST http://data-service:8081/api/v1/sql` diretamente. A validação no data-service é a única barreira nesses casos.

---

## Camada 5 — JDBC Read-Only (process-data-service)

**Última linha de defesa** — independente de qualquer código de validação.

### Sub-camada A — PRAGMA query_only (Hikari pool)

```yaml
# application.yml
spring.datasource.hikari.connection-init-sql: "PRAGMA query_only = ON"
```

Executado em toda conexão ao ser criada. O SQLite rejeita qualquer escrita naquela sessão. Aplicado ao `NamedParameterJdbcTemplate` principal (todos os endpoints exceto `/sql`).

### Sub-camada B — SQLiteOpenMode.READONLY (SqlController)

```java
// ReadOnlyDataSourceConfig.java — usado APENAS pelo SqlController
SQLiteConfig config = new SQLiteConfig();
config.setOpenMode(SQLiteOpenMode.READONLY);  // flag de SO — read-only no nível do arquivo
config.setReadOnly(true);

SQLiteDataSource ds = new SQLiteDataSource(config);
// → injetado como @Qualifier("readOnlyJdbcTemplate") no SqlController
```

`SQLiteOpenMode.READONLY` abre o arquivo com flag de sistema operacional. Mesmo que um bug no `SqlController` deixasse passar um `UPDATE`, o driver JDBC rejeitaria no nível do SO — sem possibilidade de contorno via SQL.

### Por que duas sub-camadas?

- `PRAGMA query_only` é configuração de sessão SQL — teoricamente pode ser revertida por `PRAGMA query_only = OFF` se o multi-statement não fosse bloqueado na camada 4
- `SQLiteOpenMode.READONLY` é enforced pelo SO — não pode ser revertida por nenhuma instrução SQL

---

## Configuração (application.yml)

Todos os padrões e mensagens das camadas 1–3 são externalizados em `GuardrailProperties` (`@ConfigurationProperties(prefix = "guardrail")`), sobrescritíveis por variáveis de ambiente ou application.yml.

As camadas 4–5 são hardcoded no `process-data-service` — não há configuração a ser alterada.
