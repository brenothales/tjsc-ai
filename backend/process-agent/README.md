# process-agent

Agente de IA conversacional para consulta de processos judiciais do TJSC. Expõe uma API REST que recebe perguntas em linguagem natural, orquestra chamadas a ferramentas via MCP e retorna respostas fundamentadas nos dados reais do tribunal.

## Stack

- Java 25 · Spring Boot 4.1.1 · Spring AI 2.0.1
- LLM: OpenAI GPT-4o-mini
- Memória de conversa: MongoDB
- Ferramentas: MCP Streamable HTTP (`process-mcp-server`)

## Arquitetura

```
Cliente HTTP
    │
    ▼
ChatController (/chat, /chat/stream)
    │
    ▼
AgentService (ChatClient)
    │
    ├── InputGuardrailAdvisor     ← bloqueia injeção de prompt e mensagens grandes
    ├── LanguageEnforcementAdvisor ← força resposta em português
    ├── MessageChatMemoryAdvisor  ← memória de conversa por sessão (MongoDB)
    ├── SimpleLoggerAdvisor       ← log de debug
    └── OutputGuardrailAdvisor    ← sanitiza dados sensíveis na saída
            │
            ▼
       OpenAI GPT-4o-mini
            │
            ▼
    GuardedToolCallback[]         ← valida args antes de executar cada tool
            │
            ▼
    MCP Server (process-mcp-server:8082)
```

## API

### `POST /chat`

Retorna a resposta completa em um único JSON.

**Request**
```json
{
  "conversationId": "uuid-opcional",
  "message": "qual o status do processo 0000001-43.2021.8.99.9014?"
}
```

**Response**
```json
{
  "conversationId": "2b008f3d-714a-44b5-ba3d-1e0b15693069",
  "content": "O processo 0000001-43.2021.8.99.9014 ..."
}
```

Se `conversationId` for omitido, um novo UUID é gerado automaticamente e retornado na resposta. Passe-o nas próximas mensagens para manter o contexto da conversa.

### `POST /chat/stream`

Retorna a resposta em streaming via Server-Sent Events (SSE).

```
Content-Type: text/event-stream
```

## Guardrails

Camada de segurança em 3 níveis implementada com o padrão de policies extensíveis do Spring AI Advisor API.

### Input Guardrail

Executado antes do LLM (`order = HIGHEST_PRECEDENCE`). Bloqueia a requisição e retorna resposta imediata sem consumir tokens.

| Policy | Comportamento |
|---|---|
| `PromptInjectionPolicy` | Detecta 13 padrões de injeção em português via regex |
| `MessageSizePolicy` | Rejeita mensagens acima de 2000 caracteres |

### Output Guardrail

Executado após o LLM (`order = LOWEST_PRECEDENCE`). Substitui a resposta quando detecta dado sensível.

| Policy | Detecta |
|---|---|
| `SensitiveDataOutputPolicy` | API keys (sk-proj-*), URIs MongoDB com credenciais, password/secret em texto |

### Tool Guardrail

Envolve cada `ToolCallback` MCP com `GuardedToolCallback`. Validado antes de cada chamada de ferramenta.

| Policy | Comportamento |
|---|---|
| `SqlOperationToolPolicy` | Permite apenas SELECT; bloqueia INSERT/UPDATE/DELETE/DROP e multi-statements |

**Extensibilidade:** para adicionar uma nova policy, implemente `InputPolicy`, `OutputPolicy` ou `ToolPolicy` e anote com `@Component` — o Spring injeta automaticamente via `List<Policy>`.

### Language Enforcement

`LanguageEnforcementAdvisor` injeta a instrução de idioma no system prompt via `prompt.augmentSystemMessage()` a cada requisição, garantindo respostas sempre em português independente do idioma da mensagem recebida.

## Configuração

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.0
  data:
    mongodb:
      host: ${MONGODB_HOST:localhost}
      port: ${MONGODB_PORT:27017}
      database: ${MONGODB_DATABASE:process-agent}
      username: ${MONGODB_USER:tjsc}
      password: ${MONGODB_PASS:tjsc}
      authentication-database: admin

mcp:
  server:
    url: ${MCP_SERVER_URL:http://localhost:8082/mcp}

guardrail:
  input:
    max-message-length: 2000
    injection-patterns:
      - "ignore\\s+(todas\\s+as\\s+)?instru[çc][oõ]es"
      # ... ver application.yml para lista completa
    injection-response: "Sua mensagem foi identificada como potencialmente maliciosa."
    size-exceeded-response: "Sua mensagem é muito longa."
  output:
    sensitive-patterns:
      - "sk-proj-[A-Za-z0-9_-]{20,}"
      # ... ver application.yml para lista completa
    sanitized-response: "A resposta foi filtrada por conter informações potencialmente sensíveis."

server:
  port: 8083
```

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `OPENAI_API_KEY` | Sim | — | Chave da API OpenAI |
| `MCP_SERVER_URL` | Não | `http://localhost:8082/mcp` | URL do process-mcp-server |
| `MONGODB_HOST` | Não | `localhost` | Host do MongoDB |
| `MONGODB_PORT` | Não | `27017` | Porta do MongoDB |
| `MONGODB_DATABASE` | Não | `process-agent` | Database de memória |
| `MONGODB_USER` | Não | `tjsc` | Usuário MongoDB |
| `MONGODB_PASS` | Não | `tjsc` | Senha MongoDB |

## Executando

**Pré-requisitos:** `process-mcp-server` rodando na porta 8082 e MongoDB acessível.

```bash
./mvnw spring-boot:run
```

Ou com variáveis de ambiente explícitas:

```bash
OPENAI_API_KEY=sk-... \
MCP_SERVER_URL=http://mcp-server:8082/mcp \
./mvnw spring-boot:run
```

## Testes

```bash
# Apenas testes unitários dos guardrails (sem dependências externas)
./mvnw test -Dtest="**/guardrail/**"

# Todos os testes
./mvnw test
```

60 testes unitários cobrindo todas as policies e advisors de guardrail.

## Dependências de runtime

| Serviço | Porta padrão | Papel |
|---|---|---|
| `process-mcp-server` | 8082 | Ferramentas MCP (dados dos processos) |
| MongoDB | 27017 | Memória de conversa persistente |
| OpenAI API | — | Modelo de linguagem |
