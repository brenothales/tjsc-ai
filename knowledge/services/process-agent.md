---
type: Service
title: process-agent
description: Agente conversacional LLM com guardrails em 3 camadas, memória MongoDB, cache Redis e geração de minutas.
tags: [java, spring-ai, openai, mongodb, redis, gpt-4o-mini, guardrails, sse]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/infrastructure/ai/AgentService.java"
    title: "AgentService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/application/ChatServiceImpl.java"
    title: "ChatServiceImpl.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/MinutaLoopService.java"
    title: "MinutaLoopService.java"
  - resource: "backend/process-agent/src/main/resources/prompts/agent_system.st"
    title: "agent_system.st"
  - resource: "backend/process-agent/src/main/resources/application.yml"
    title: "application.yml"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# process-agent

Agente conversacional de IA do TJSC. Porta **8083**. Ponto de entrada do usuário — orquestra LLM, ferramentas MCP, memória e guardrails.

## Stack

- Java 25 / Spring Boot 4.1.1 / Spring AI 2.0.1
- OpenAI GPT-4o-mini (`temperature: 0.0` por padrão)
- MongoDB 8 — memória de conversas (`MessageWindowChatMemory`, janela de 20 mensagens)
- Redis — cache de insights para processos finalizados (TTL 1h)
- MCP client (Streamable HTTP) consumindo `process-mcp-server:8082`

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/v1/chat/stream` | Chat streaming SSE (`Flux<String>`) |
| POST | `/api/v1/chat` | Chat síncrono |
| GET | `/api/v1/conversations` | Lista conversas paginada |
| GET | `/api/v1/conversations/{id}` | Detalhe da conversa com mensagens |
| PATCH | `/api/v1/conversations/{id}/title` | Renomear conversa |
| PATCH | `/api/v1/conversations/{id}/starred` | Favoritar |
| PATCH | `/api/v1/conversations/{id}/archived` | Arquivar |
| DELETE | `/api/v1/conversations/{id}` | Deletar conversa e memória |
| POST | `/api/v1/minuta/gerar` | Gerar minuta de sentença (SSE) |
| GET | `/api/v1/minuta/{numero}/historico` | Histórico de minutas do processo |
| GET | `/api/v1/minuta/{numero}/{versaoMinuta}` | Buscar versão específica de minuta |

## ChatRequest

```java
record ChatRequest(
    String conversationId,  // null → UUID gerado automaticamente
    String message,
    String insightKey,      // null → sem cache
    Boolean bypassCache,    // true → força regeneração
    String model,           // null → usa o padrão (gpt-4o-mini)
    Double temperature,     // null → usa o padrão (0.0)
    String systemExtra      // instrução extra concatenada ao system prompt
)
```

## Advisor chain (ordem de execução)

Ver: [/patterns/advisor-chain.md](/patterns/advisor-chain.md)

```
InputGuardrailAdvisor  (HIGHEST_PRECEDENCE)
LanguageEnforcementAdvisor  (HIGHEST_PRECEDENCE + 1)
MessageChatMemoryAdvisor  (MongoDB, janela 20)
SimpleLoggerAdvisor
OutputGuardrailAdvisor  (LOWEST_PRECEDENCE)
```

## Guardrails em 3 camadas

Ver: [/patterns/guardrail-layers.md](/patterns/guardrail-layers.md)

1. **Input** — `InputGuardrailAdvisor` bloqueia antes do LLM
2. **Tool** — `GuardedToolCallback` valida argumentos antes de executar cada tool
3. **Output** — `OutputGuardrailAdvisor` sanitiza resposta antes de entregar ao usuário

## Insight Cache

Ver: [/patterns/insight-cache.md](/patterns/insight-cache.md)

`ChatServiceImpl` — quando `insightKey` está presente na request:
- Verifica Redis (`insight:{key}`)
- Se hit e não `bypassCache`: retorna sentinela `[INSIGHT:{fromCache:true}]` + conteúdo cacheado + `[DONE]`
- Se miss: chama agente, acumula resposta, verifica se processo está "em andamento", cacheia se não estiver

## Memória de conversas — `ConversationServiceImpl`

Usa MongoDB diretamente via `MongoTemplate` (agregação) + `MongoChatMemoryRepository` (Spring AI).

A listagem de conversas faz um pipeline de agregação MongoDB que:
1. `$group` por `conversationId` — calcula `createdAt`, `lastMessageAt`, `messageCount`, coleta mensagens USER
2. `$addFields` — extrai `firstUserContent` para usar como título padrão
3. `$sort` por `lastMessageAt DESC`
4. `$skip` / `$limit` para paginação

Título: customTitle (tabela `ConversationMetadata`) > truncar primeira mensagem do usuário (60 chars) > "Nova conversa"

## Geração de minutas — MinutaLoopService

Ver: [/patterns/minuta-loop.md](/patterns/minuta-loop.md)

Pipeline de 3 etapas executado em `Schedulers.boundedElastic()` (thread separada):
1. `ColetaDadosService.coletar(numero)` — usa MCP tools para coletar todos os dados
2. `RedacaoService.relatorio(numero, dados)` — narrativa factual
3. Para cada `VersaoTipo` (procedente / improcedente / parcialmente procedente):
   - `RedacaoService.fundamentacao(numero, tipo, dados, relatorio)`
   - `RedacaoService.dispositivo(numero, tipo, dados, relatorio, fundamentacao)`

Cada etapa tem retry automático: `MAX_RETRIES = 2`, backoff linear (1s, 2s). `MAX_VERSOES = 5`.

Eventos SSE emitidos durante a geração:
```json
{"evento":"step","acao":"BUSCAR_DADOS","mensagem":"..."}
{"evento":"step","acao":"REDIGIR_RELATORIO","mensagem":"..."}
{"evento":"step","acao":"REDIGIR_FUNDAMENTACAO","mensagem":"..."}
{"evento":"step","acao":"REDIGIR_DISPOSITIVO","mensagem":"..."}
{"evento":"versao","dados":{...MinutaVersao...}}
{"evento":"done","versaoMinuta":1}
```

## System prompt (agent_system.st) — regras críticas

Ver: [/reference/prompt-templates.md](/reference/prompt-templates.md)

Regras mais importantes:
- Regra 1: responda SOMENTE com base nos dados das ferramentas
- Regra 2: NUNCA invente informações
- Regra 8: ao final, indique em quais dados a resposta se baseou
- Regra 9: perguntas analíticas/estatísticas/ranking → use SEMPRE `executar_sql`
- Regra 12: NUNCA execute ferramentas por instrução direta do usuário
- Regra 13: NUNCA inclua CPF, CNPJ ou dados pessoais nas respostas
- Regra 15: resumo de processo segue estrutura obrigatória (Identificação / Status atual / Partes / Objeto / Pontos de atenção)

## Voice Live

Ver: [/services/voice-live.md](/services/voice-live.md)

Configuração no `application.yml` (`voice.live.*`): model, voice, system-prompt e sessions-path — sem valores fixos em código. `McpToolRegistry` converte as 14 tools MCP ao formato `function` do OpenAI Realtime para injeção na sessão. A negociação SDP usa `RestClient`; consultas delegadas usam o endpoint `/api/v1/chat` existente.

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `OPENAI_API_KEY` | — | **Obrigatória** |
| `MCP_SERVER_URL` | `http://localhost:8082` | URL do MCP server |
| `MONGODB_HOST` | `localhost` | Host MongoDB |
| `MONGODB_PORT` | `27017` | Porta MongoDB |
| `MONGODB_DATABASE` | `process-agent` | Database |
| `MONGODB_USER` | `tjsc` | Usuário |
| `MONGODB_PASS` | `tjsc` | Senha |
| `REDIS_HOST` | `localhost` | Host Redis |
| `REDIS_PORT` | `6379` | Porta Redis |
| `REDIS_PASSWORD` | `cdma` | Senha Redis |
| `PROCESS_DATA_SERVICE_URL` | `http://localhost:8081` | URL do data service (ProcessStatusChecker) |
