---
type: Reference
title: Diagrama de Arquitetura — Infraestrutura Docker
description: Topologia de serviços Docker Compose do TJSC AI — containers, portas, dependências, volumes e rede tjsc_net.
tags: [arquitetura, docker, infraestrutura, serviços]
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Diagrama de Arquitetura — Infraestrutura Docker

Oito serviços na rede `tjsc_net` (bridge). O frontend é o único ponto de entrada do usuário; o process-agent é o único que fala com a OpenAI externamente.

## Topologia de Serviços

```mermaid
graph TD
    User(["Usuário\n(Browser)"])

    subgraph ext["Externo"]
        OAI(["OpenAI API\nGPT-4o-mini"])
    end

    subgraph docker["Docker — tjsc_net (bridge)"]
        FE["<b>frontend</b>\nhost:4200 → :80\nNginx + Angular 21"]

        AGENT["<b>process-agent</b>\n:8083\nSpring AI · Advisors · Guardrails\nMinuta Loop · SSE Streaming"]

        MCP["<b>process-mcp-server</b>\n:8082\n14 ferramentas MCP\nStreamable HTTP"]

        DATA["<b>process-data-service</b>\n:8081\nREST · SQLite read-only\nSqlController · JDBC READONLY"]

        MONGO[("<b>MongoDB</b>\n:27017\nChatMemory · Minutas")]
        REDIS[("<b>Redis</b>\n:6379\nInsight Cache TTL 1h")]

        ME["mongo-express\n:8084\nadmin UI"]
        RI["redis_insight\n:8001\nadmin UI"]

        SQLITE[("SQLite\nprocessos.db\nbind mount")]
    end

    User -->|"HTTP · SSE"| FE
    FE -->|"HTTP /api/v1/ (proxy)"| AGENT
    AGENT -->|"MCP Streamable HTTP /mcp"| MCP
    AGENT -->|"ChatMemory\n(janela 20 msgs)"| MONGO
    AGENT -->|"Insight Cache\n(por nº processo)"| REDIS
    MCP -->|"HTTP REST /api/v1/"| DATA
    DATA -->|"JDBC READONLY\nPRAGMA query_only=ON"| SQLITE
    AGENT -->|"HTTPS\nOpenAI SDK"| OAI

    MONGO -.-|"admin"| ME
    REDIS -.-|"admin"| RI
```

## Dependências (depends_on)

| Serviço | Aguarda |
|---------|---------|
| `mongo-express` | `mongodb` healthy |
| `redis_insight` | `redis` healthy |
| `process-mcp-server` | `process-data-service` healthy |
| `process-agent` | `mongodb` healthy · `redis` healthy · `process-mcp-server` started |
| `frontend` | `process-agent` started |

## Portas e Healthchecks

| Serviço | Porta | Healthcheck |
|---------|-------|-------------|
| `mongodb` | 27017 | `mongosh --eval "db.adminCommand('ping')"` |
| `mongo-express` | 8084 | — |
| `redis` | 6379 | `redis-cli ping` |
| `redis_insight` | 8001 | — |
| `process-data-service` | 8081 | `curl /actuator/health` |
| `process-mcp-server` | 8082 | — |
| `process-agent` | 8083 | — |
| `frontend` | 4200→80 | — |

## Volumes

| Volume | Usado por | Conteúdo |
|--------|-----------|----------|
| `./backend/mongodb:/data/db` | mongodb | dados persistentes (bind mount) |
| `redis_volume_data` | redis | dados do cache |
| `redis_insight_volume_data` | redis_insight | config do painel |

## Variáveis de ambiente principais (process-agent)

| Variável | Destino |
|----------|---------|
| `SPRING_AI_OPENAI_API_KEY` | OpenAI API |
| `SPRING_DATA_MONGODB_URI` | `mongodb://mongodb:27017/tjsc` |
| `SPRING_DATA_REDIS_HOST` | `redis` |
| `MCP_SERVER_URL` | `http://process-mcp-server:8082/mcp` |
