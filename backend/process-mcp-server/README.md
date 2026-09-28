# process-mcp-server

Servidor MCP (Model Context Protocol) para consulta de processos judiciais do TJSC.  
Expõe ferramentas que permitem a agentes de IA consultarem dados processuais de forma autônoma,
delegando as queries ao `process-data-service` via HTTP.

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Spring AI MCP Server | 2.0.1 |
| Protocolo MCP | Streamable HTTP |

## Arquitetura

```
Agent (LLM)
    │  MCP protocol (SSE/Streamable HTTP)
    ▼
process-mcp-server  :8082
    │  HTTP REST
    ▼
process-data-service  :8081
    │  JDBC
    ▼
desafio.sqlite
```

O MCP server atua como adaptador: traduz chamadas de ferramentas do protocolo MCP em requisições REST para o `process-data-service`. Não acessa o banco diretamente.

## Ferramentas disponíveis

### Consulta por processo individual

| Ferramenta | Descrição |
|---|---|
| `buscar_contexto_completo` | Retorna todos os dados do processo em uma única chamada: básicos, partes, advogados, magistrado, movimentações, sentença e petição inicial. Ponto de entrada preferencial. |
| `buscar_processo` | Dados básicos: classe, assunto, comarca, vara, datas, valor da causa e situação. |
| `buscar_partes` | Lista autor(es), réu(s) e demais participantes com polo e documento. |
| `buscar_advogados` | Advogados identificados nos documentos com nome e OAB. |
| `buscar_magistrado` | Magistrado responsável pelo processo. |
| `buscar_movimentacoes` | 50 movimentações mais recentes, ordenadas por data decrescente. |
| `buscar_documentos` | Lista os 50 documentos mais recentes (sem texto integral). |
| `buscar_documento_por_id` | Texto integral de um documento pelo seu ID. |
| `buscar_documentos_com_texto` | Documentos com texto integral, com filtro opcional por tipo (Sentença, Contestação, Despacho, etc.). |
| `buscar_sentenca` | Sentença principal com texto integral. |
| `buscar_peticao_inicial` | Petição inicial com texto integral. |
| `buscar_decisao` | Decisão principal: tipo e data, sem texto integral. |

### Pesquisa e análise

| Ferramenta | Descrição |
|---|---|
| `pesquisar_processos` | Pesquisa por nome de parte, classe, comarca e/ou situação. Todos os filtros opcionais (envie string vazia). Retorna até 20 resultados. |
| `executar_sql` | Executa uma consulta SELECT diretamente no banco SQLite. Use para perguntas analíticas: rankings, contagens, médias, distribuições. Apenas SELECT é permitido. |

### Esquema do banco (para `executar_sql`)

```sql
processo     — id, numero, classe, assunto, comarca, dt_aut, data_sentenca, valor_causa, situacao
parte        — id, nome, tipo, documento
processo_parte — processo_id, parte_id, polo (ATIVO/PASSIVO)
documento    — id, processo_id, tipo, autor, dt_juntada, texto
movimentacao — id, processo_id, descricao, dt_movimentacao
magistrado   — id, nome
unidade      — id, nome, comarca
```

> **Atenção:** o campo `situacao` tem casing inconsistente no banco. Use sempre `LOWER(situacao)` para filtrar por situação.  
> Exemplo: `WHERE LOWER(p.situacao) = 'julgado'`

## Configuração

### `application.yml`

```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        streamable-http:
          mcp-endpoint: /mcp

server:
  port: 8082

process:
  data:
    service:
      url: ${PROCESS_DATA_SERVICE_URL:http://localhost:8081}
```

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `PROCESS_DATA_SERVICE_URL` | `http://localhost:8081` | URL base do process-data-service |

## Como executar

### Pré-requisito

O `process-data-service` deve estar rodando em `:8081` antes de iniciar este serviço.

### Maven

```bash
./mvnw spring-boot:run
```

### Jar

```bash
./mvnw package -DskipTests
java --enable-preview -jar target/process-mcp-server-0.0.1-SNAPSHOT.jar
```

## Endpoint MCP

```
POST http://localhost:8082/mcp
```

Compatível com o protocolo MCP Streamable HTTP (2024-11-05).  
Requer os headers `Accept: application/json, text/event-stream` e `mcp-session-id` após inicialização.

### Inicializar sessão

```bash
curl -D - -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": { "name": "test", "version": "1.0" }
    }
  }'
```

O header `mcp-session-id` da resposta deve ser enviado em todas as chamadas seguintes.

### Listar ferramentas

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "mcp-session-id: <session-id>" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

### Chamar uma ferramenta

```bash
curl -X POST http://localhost:8082/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "mcp-session-id: <session-id>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "executar_sql",
      "arguments": {
        "sql": "SELECT comarca, COUNT(*) as total FROM processo GROUP BY comarca ORDER BY total DESC LIMIT 5"
      }
    }
  }'
```