---
type: Decision
title: Divisão em 3 serviços backend
description: Por que process-data-service, process-mcp-server e process-agent são serviços separados em vez de um monólito.
tags: [arquitetura, microserviços, separação-de-responsabilidades]
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Decisão: Divisão em 3 Serviços Backend

## Decisão

O backend foi dividido em 3 serviços com portas e responsabilidades distintas:
- `process-data-service:8081` — acesso aos dados SQLite
- `process-mcp-server:8082` — protocolo MCP para o agente
- `process-agent:8083` — LLM, memória, guardrails, minuta

## Por que não um monólito?

**process-data-service não deve depender de IA.**

O serviço de dados é testável de forma completamente independente: seus 53 testes de integração rodam contra o SQLite real sem nenhuma dependência de LLM, Spring AI ou MCP. Se o acesso a dados ficasse no mesmo serviço que o LLM, um teste de repositório precisaria de contexto Spring AI completo — lento, frágil, caro.

Além disso, a mesma API HTTP pode ser consumida por:
- `process-mcp-server` (caso atual)
- Interface REST direta / Swagger UI
- Outros microsserviços futuros
- Ferramentas de auditoria

**process-mcp-server é uma camada de tradução.**

O MCP server não sabe nada sobre SQLite ou Spring AI. Ele converte chamadas MCP em chamadas HTTP ao data service. Se o protocolo MCP evoluir, só esse serviço muda. Se quisermos expor as mesmas ferramentas via outro protocolo (gRPC, por exemplo), criamos outro servidor sem tocar nos dados.

**process-agent é o único que conhece LLM.**

Isolando a lógica conversacional (guardrails, memória MongoDB, streaming SSE, geração de minutas) no agent, é possível:
- Trocar o LLM (GPT-4o-mini → Claude → Gemini) sem afetar os dados
- Trocar o framework de IA (Spring AI → LangChain4j) sem afetar o MCP server
- Escalar o agent independentemente dos dados

## Trade-off aceito

Latência adicional: cada pergunta percorre agent → mcp-server → data-service (2 hops HTTP além do LLM). Para o contexto do desafio (localhost, usuário único), o impacto é imperceptível. Em produção com volumes altos, o data-service poderia ser consumido diretamente pelo agent para consultas simples.
