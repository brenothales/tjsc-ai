---
type: Service
title: process-mcp-server
description: Servidor MCP que expõe 14 ferramentas de consulta processual ao agente de IA.
tags: [java, spring-ai, mcp, spring-boot]
sources:
  - resource: "backend/process-mcp-server/src/main/java/br/jus/tjsc/ai/mcp/ProcessoTools.java"
    title: "ProcessoTools.java"
  - resource: "backend/process-mcp-server/src/main/java/br/jus/tjsc/ai/mcp/ProcessDataClient.java"
    title: "ProcessDataClient.java"
  - resource: "backend/process-mcp-server/src/main/resources/application.yml"
    title: "application.yml"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# process-mcp-server

Servidor MCP (Model Context Protocol) que expõe 14 ferramentas de consulta processual. Porta **8082**. Não acessa SQLite diretamente — delega ao `process-data-service` via HTTP.

## Stack

- Java 25 / Spring Boot 4.1.1 / Spring AI MCP Server 2.0.1
- Protocolo: MCP Streamable HTTP (2024-11-05)
- Endpoint MCP: `POST http://localhost:8082/mcp`
- `ToolCallback[]` injetado no `process-agent` via MCP client

## Arquitetura

```
LLM (process-agent)
    │  MCP Streamable HTTP
    ▼
ProcessoTools (@McpTool)
    └── ProcessDataClient
         ├── ProcessDataApi  (HTTP Exchange → /api/v1/processos/*)
         └── SqlDataApi      (HTTP Exchange → POST /api/v1/sql)
```

`ProcessDataClient.call(Supplier)` centraliza tratamento de erros HTTP: 404 → `{found:false}`, 400 → `{found:false, message: "Número inválido"}`, outros → `{found:false, message: "Erro HTTP ..."}`.

## Ferramentas disponíveis

Ver catálogo completo: [/reference/mcp-tools-catalog.md](/reference/mcp-tools-catalog.md)

| Ferramenta | Quando usar |
|---|---|
| `buscar_contexto_completo` | Ponto de entrada padrão — "me explique o processo", "o que aconteceu?" |
| `buscar_processo` | Só dados básicos sem partes/movimentações |
| `buscar_partes` | "quem são as partes?", "quem é o réu?" |
| `buscar_advogados` | "qual é o advogado?", "número OAB?" |
| `buscar_magistrado` | "quem é o juiz?", "quem assinou?" |
| `buscar_movimentacoes` | SEMPRE que perguntar sobre andamento — mesmo após buscar_contexto |
| `buscar_documentos` | Listar tipos sem texto integral |
| `buscar_documento_por_id` | Texto de doc específico por ID |
| `buscar_documentos_com_texto` | Docs filtrados por tipo com texto integral |
| `buscar_sentenca` | "qual foi a sentença?", "já foi julgado?" |
| `buscar_peticao_inicial` | "quais são os pedidos?", "como foi iniciado?" |
| `buscar_decisao` | Metadados de decisão sem texto |
| `pesquisar_processos` | Busca por parte/classe/comarca/situação |
| `executar_sql` | Perguntas analíticas: rankings, contagens, comparações |


## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `PROCESS_DATA_SERVICE_URL` | `http://localhost:8081` | URL do process-data-service |
