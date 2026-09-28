# process-data-service

API REST somente leitura para consulta de dados processuais do TJSC.  
Camada de acesso a dados da solução de IA conversacional — sem dependência de IA ou MCP.

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Spring Web + JDBC + Validation + Actuator | — |
| SQLite JDBC | 3.49.1.0 |
| springdoc-openapi | 2.8.9 |

## Arquitetura

Hexagonal (Ports and Adapters):

```
adapter/in/web           → Controllers REST (ProcessoController, SqlController)
application/port/in      → Interfaces de caso de uso
application/service      → Implementação dos casos de uso
application/port/out     → Interface do repositório
adapter/out/sqlite       → Implementação JDBC + SQLite
domain/model             → Entidades e normalizadores
domain/exception         → Exceções de domínio
config/                  → ReadOnlyDataSourceConfig, GlobalExceptionHandler, OpenApiConfig
```

## Pré-requisitos

- Java 25+
- Arquivo `desafio.sqlite` na raiz do projeto pai (`../desafio.sqlite`)

## Como executar

```bash
./mvnw spring-boot:run
```

Com caminho customizado do banco:

```bash
SQLITE_DATABASE_PATH=/caminho/para/desafio.sqlite ./mvnw spring-boot:run
```

Porta padrão: **8081**  
Swagger UI: `http://localhost:8081/swagger-ui.html`

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SQLITE_DATABASE_PATH` | `../desafio.sqlite` | Caminho do banco SQLite |

## Endpoints

### Processo

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/v1/api/processos/{numero}` | Dados básicos do processo |
| `GET` | `/v1/api/processos/{numero}/contexto` | Todos os dados em uma chamada (processo + partes + advogados + magistrado + movimentações + sentença + petição) |
| `GET` | `/v1/api/processos/{numero}/partes` | Autor(es), réu(s) e demais participantes |
| `GET` | `/v1/api/processos/{numero}/advogados` | Advogados identificados nos documentos (OAB) |
| `GET` | `/v1/api/processos/{numero}/magistrados` | Magistrado responsável |
| `GET` | `/v1/api/processos/{numero}/movimentacoes` | Movimentações com paginação |
| `GET` | `/v1/api/processos/{numero}/documentos` | Lista de documentos sem texto (paginado) |
| `GET` | `/v1/api/processos/{numero}/documentos/{documentoId}` | Texto integral de um documento por ID |
| `GET` | `/v1/api/processos/{numero}/documentos/texto` | Documentos com texto integral, filtro por `?tipo=` |
| `GET` | `/v1/api/processos/{numero}/sentenca` | Sentença principal com texto integral |
| `GET` | `/v1/api/processos/{numero}/peticao` | Petição inicial com texto integral |
| `GET` | `/v1/api/processos/{numero}/decisao` | Decisão principal (tipo e data, sem texto) |
| `GET` | `/v1/api/processos/pesquisar` | Pesquisa por parte, classe, comarca e/ou situação |

**Parâmetros de paginação** (movimentações e documentos):

```
?page=0&size=20    (padrão)
?page=1&size=50    (máximo: size=100)
```

**Parâmetros de pesquisa** (`/pesquisar`):

```
?parte=Silva               (busca parcial por nome)
?classe=Procedimento Comum Cível
?comarca=Florianópolis
?situacao=julgado          (busca parcial, case-insensitive)
?page=0&size=20
```

**Formatos aceitos para `{numero}`:**

```
0000001-02.2019.8.24.0000       (CNJ com pontuação)
00000010220198240000            (20 dígitos contínuos)
```

### SQL analítico

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/v1/api/sql` | Executa uma query SELECT no banco SQLite |

Body:
```json
{ "sql": "SELECT comarca, COUNT(*) as total FROM processo GROUP BY comarca ORDER BY total DESC LIMIT 10" }
```

Resposta:
```json
{
  "success": true,
  "error": null,
  "rows": [
    { "comarca": "Florianópolis", "total": 954 },
    { "comarca": "Joinville", "total": 844 }
  ],
  "count": 2
}
```

> Endpoint para uso interno pelo `process-mcp-server`. Não deve ser exposto publicamente em produção.

## Exemplos de uso

### Buscar processo

```bash
curl http://localhost:8081/v1/api/processos/0000001-02.2019.8.24.0000
```

### Contexto completo

```bash
curl http://localhost:8081/v1/api/processos/0000001-02.2019.8.24.0000/contexto
```

### Movimentações (página 2, 10 itens)

```bash
curl "http://localhost:8081/v1/api/processos/0000001-02.2019.8.24.0000/movimentacoes?page=1&size=10"
```

### Documentos com texto, filtro por tipo

```bash
curl "http://localhost:8081/v1/api/processos/0000001-02.2019.8.24.0000/documentos/texto?tipo=Contestação"
```

### Pesquisar processos

```bash
curl "http://localhost:8081/v1/api/processos/pesquisar?parte=Silva&comarca=Florianópolis&situacao=julgado"
```

### Consulta SQL analítica

```bash
curl -X POST http://localhost:8081/v1/api/sql \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT classe, COUNT(*) as total FROM processo GROUP BY classe ORDER BY total DESC LIMIT 5"}'
```

## Testes

```bash
./mvnw test
```

53 testes cobrindo:
- Normalizador de número CNJ (incluindo tentativas de SQL injection)
- Application services com mock do repositório
- Repositório SQLite contra banco de teste real
- Endpoints REST com MockMvc

## Segurança

### Endpoints de processo
- Número CNJ validado antes de qualquer acesso ao banco
- Todas as queries usam parâmetros nomeados — sem concatenação de strings
- Conexão principal configurada com `PRAGMA query_only = ON` via Hikari `connection-init-sql`
- Stack traces nunca expostos na API

### Endpoint `/v1/api/sql`
Utiliza uma conexão **dedicada em modo read-only** (`SQLiteOpenMode.READONLY`) separada do pool principal, com três camadas de validação:

| Camada | Proteção |
|---|---|
| `startsWith("SELECT")` | Bloqueia DELETE, UPDATE, INSERT, DROP, USE, etc. diretos |
| `contains(";")` | Bloqueia multi-statement (`SELECT 1; DROP TABLE ...`) |
| Blocklist de keywords | Bloqueia keywords perigosas embutidas no SELECT (INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, TRUNCATE, REPLACE, ATTACH, DETACH, PRAGMA, VACUUM, REINDEX, ANALYZE, USE) |
| `SQLiteOpenMode.READONLY` | Última linha de defesa — qualquer escrita falha no nível do driver |

Limite máximo de 100 linhas por query (aplicado automaticamente se `LIMIT` não estiver presente).

## Modelo de dados

Ver [`DATA_MODEL.md`](DATA_MODEL.md) para o diagrama ER completo, descrição de cada tabela e queries relevantes.

> **Atenção:** o campo `situacao` da tabela `processo` tem casing inconsistente no banco (`julgado`, `Julgado`, `JULGADO`, etc.). Use sempre `LOWER(situacao)` em filtros SQL.

## Health check

```bash
curl http://localhost:8081/actuator/health
```
