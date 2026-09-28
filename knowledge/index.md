---
okf_version: "0.2"
---

# TJSC — Agente de Consulta Processual

Knowledge base do sistema de IA conversacional para consulta de processos judiciais do TJSC.

## Serviços

- [process-data-service](/services/process-data-service.md) — API REST somente leitura sobre SQLite
- [process-mcp-server](/services/process-mcp-server.md) — 14 ferramentas MCP sobre o data service
- [process-agent](/services/process-agent.md) — Agente conversacional LLM + guardrails + memória
- [frontend-tjsc-ai](/services/frontend-tjsc-ai.md) — Chat Angular 21 com SSE streaming

## Padrões

- [Advisor Chain e Guardrails](/patterns/advisor-chain.md) — 5 advisors em ordem determinística
- [Guardrail em 5 Camadas](/patterns/guardrail-layers.md) — input, tool, output (agente) + SqlController + JDBC read-only (banco)
- [Minuta Loop (multi-step agent)](/patterns/minuta-loop.md) — coleta → relatório → versões
- [Diagrama do Minuta Loop](/patterns/minuta-loop-diagram.md) — sequenceDiagram Mermaid completo com retry e eventos SSE
- [Insight Cache Redis](/patterns/insight-cache.md) — cache condicional por situação do processo
- [SSE Streaming](/patterns/sse-streaming.md) — Flux<String> + sentinelas + ReadableStream

## Referências

- [Diagrama de Arquitetura — Infraestrutura Docker](/reference/architecture-infra.md) — 8 serviços, portas, dependências, volumes
- [Schema do banco SQLite](/reference/database-schema.md) — 7 tabelas, volumes e índices
- [Catálogo das 14 ferramentas MCP](/reference/mcp-tools-catalog.md) — nome, descrição, parâmetros
- [Prompt Templates](/reference/prompt-templates.md) — agent_system, coleta, redação, minuta
- [Padrões de Injeção Bloqueados](/reference/injection-patterns.md) — regex do guardrail de input

## Decisões

- [Divisão em 3 serviços](/decisions/three-service-split.md) — por que separar data / mcp / agent
- [SQLite somente leitura](/decisions/sqlite-readonly.md) — duas camadas de proteção contra escrita
