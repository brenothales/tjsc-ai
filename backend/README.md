# TJSC — Agente de Consulta Processual

Sistema de IA conversacional para consulta de processos judiciais do Tribunal de Justiça de Santa Catarina.  
O usuário faz perguntas em português natural e o agente responde com dados reais do banco, indicando as fontes utilizadas.

## Visão geral

```
┌─────────────────────────────────────────────────┐
│                  Usuário                        │
│         (perguntas em português natural)        │
└────────────────────┬────────────────────────────┘
                     │ HTTP REST
                     ▼
┌─────────────────────────────────────────────────┐
│             process-agent  :8083                │
│                                                 │
│  ChatClient (Spring AI)                         │
│  ├── GPT-4o-mini                                │
│  ├── MessageChatMemoryAdvisor (MongoDB)         │
│  └── MCP Tools (14 ferramentas)                 │
└────────────────────┬────────────────────────────┘
                     │ MCP Streamable HTTP
                     ▼
┌─────────────────────────────────────────────────┐
│          process-mcp-server  :8082              │
│                                                 │
│  14 ferramentas MCP:                            │
│  buscar_contexto_completo, buscar_processo,     │
│  buscar_partes, buscar_movimentacoes,           │
│  executar_sql, pesquisar_processos, ...         │
└────────────────────┬────────────────────────────┘
                     │ HTTP REST
                     ▼
┌─────────────────────────────────────────────────┐
│         process-data-service  :8081             │
│                                                 │
│  API REST somente leitura                       │
│  Arquitetura hexagonal                          │
│  53 testes de integração                        │
└────────────────────┬────────────────────────────┘
                     │ JDBC (read-only)
                     ▼
              desafio.sqlite
          (10.000 processos, 7 tabelas)
```

## Serviços

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `process-data-service` | 8081 | API REST somente leitura sobre o banco SQLite |
| `process-mcp-server` | 8082 | Expõe as ferramentas de consulta via protocolo MCP |
| `process-agent` | 8083 | Agente conversacional com LLM + memória + MCP tools |
| MongoDB | 27017 | Persistência da memória das conversas |

## Capacidades do agente

1. **Entender** — interpreta perguntas em português natural sobre processos judiciais
2. **Consultar** — chama ferramentas MCP de forma autônoma para buscar os dados necessários
3. **Fundamentar** — indica ao final de cada resposta em quais dados ela se baseou
4. **Conversar** — preserva contexto entre múltiplos turnos via memória persistida no MongoDB
5. **Reconhecer limites** — declara quando os dados disponíveis não permitem responder

## Stack técnica

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.1 |
| IA | Spring AI 2.0.1 + GPT-4o-mini |
| Protocolo de ferramentas | MCP Streamable HTTP |
| Memória conversacional | MongoDB 8 via `MongoChatMemoryRepository` |
| Banco de dados | SQLite (`desafio.sqlite`) |
| Testes | JUnit 5 + Spring Boot Test (53 testes) |

## Pré-requisitos

- Java 25+
- Maven 3.9+
- Docker (para o MongoDB)
- Chave de API da OpenAI

## Como executar

### 1. Subir o MongoDB

```bash
docker-compose up -d
```

Isso sobe o MongoDB na porta `27017` e o Mongo Express (interface web) na porta `8084`.

### 2. Configurar a chave OpenAI

Exporte a variável de ambiente antes de iniciar o `process-agent`:

```bash
export OPENAI_API_KEY=sk-...
```

### 3. Iniciar os serviços (em terminais separados)

```bash
# Terminal 1
cd process-data-service && ./mvnw spring-boot:run

# Terminal 2
cd process-mcp-server && ./mvnw spring-boot:run

# Terminal 3
cd process-agent && ./mvnw spring-boot:run
```

A ordem importa: `process-data-service` → `process-mcp-server` → `process-agent`.

## Variáveis de ambiente

| Variável | Serviço | Padrão | Descrição |
|---|---|---|---|
| `OPENAI_API_KEY` | process-agent | — | Chave da API OpenAI (obrigatória) |
| `MCP_SERVER_URL` | process-agent | `http://localhost:8082/mcp` | URL do MCP server |
| `MONGODB_HOST` | process-agent | `localhost` | Host do MongoDB |
| `MONGODB_PORT` | process-agent | `27017` | Porta do MongoDB |
| `MONGODB_USER` | process-agent | `tjsc` | Usuário do MongoDB |
| `MONGODB_PASS` | process-agent | `tjsc` | Senha do MongoDB |
| `MONGODB_DATABASE` | process-agent | `process-agent` | Database do MongoDB |
| `PROCESS_DATA_SERVICE_URL` | process-mcp-server | `http://localhost:8081` | URL do data service |
| `SQLITE_DATABASE_PATH` | process-data-service | `../desafio.sqlite` | Caminho do banco SQLite |

## API do agente

### Chat (síncrono)

```bash
curl -X POST http://localhost:8083/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "minha-sessao-01",
    "message": "me explica o processo 0000001-02.2019.8.24.0000"
  }'
```

Resposta:
```json
{
  "conversationId": "minha-sessao-01",
  "content": "O processo 0000001-02.2019.8.24.0000 ..."
}
```

### Chat (streaming SSE)

```bash
curl -N -X POST http://localhost:8083/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "conversationId": "minha-sessao-01",
    "message": "quais foram as movimentações?"
  }'
```

> O `conversationId` é opcional. Se omitido, uma nova sessão é criada automaticamente.  
> Para manter contexto entre turnos, reutilize o mesmo `conversationId`.

## Exemplos de perguntas

### Consulta de processo específico
```
me explica o processo 0000001-02.2019.8.24.0000
quais foram as movimentações desse processo?
o que diz a sentença?
quem são as partes?
qual o advogado do autor?
```

### Pesquisa
```
processos do João Silva em Florianópolis
processos de Execução Fiscal já julgados em Joinville
```

### Análise (gera SQL automaticamente)
```
qual comarca tem mais processos?
quantos processos a magistrada Sandra Rodrigues Laurindo já julgou?
qual a média de dias entre autuação e sentença por comarca?
distribuição dos processos por faixa de valor de causa
quais classes processuais são mais comuns?
```

### Reconhecimento de limites
```
qual a previsão do tempo em Florianópolis?   → agente declara que não tem essa informação
```

## Segurança

O endpoint `/v1/api/sql` aceita apenas queries `SELECT`. Além disso, utiliza uma conexão SQLite dedicada em modo `READONLY` (`SQLiteOpenMode.READONLY`), garantindo que nenhuma escrita seja possível mesmo em tentativas de bypass via multi-statement (`SELECT 1; DROP TABLE ...`).

## Documentação adicional

- [`process-mcp-server/README.md`](process-mcp-server/README.md) — detalhes das 14 ferramentas MCP, schema do banco e exemplos de chamada via protocolo MCP
- Swagger UI do data service: `http://localhost:8081/swagger-ui.html`
