---
type: Service
title: process-data-service
description: API REST somente leitura sobre o banco SQLite de processos judiciais do TJSC.
tags: [java, spring-boot, sqlite, hexagonal, rest-api]
sources:
  - resource: "backend/process-data-service/src/main/java/br/jus/tjsc/ai/process/adapter/in/web/ProcessoController.java"
    title: "ProcessoController.java"
  - resource: "backend/process-data-service/src/main/java/br/jus/tjsc/ai/process/adapter/out/sqlite/SQLiteProcessoRepository.java"
    title: "SQLiteProcessoRepository.java"
  - resource: "backend/process-data-service/src/main/resources/application.yml"
    title: "application.yml"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# process-data-service

API REST somente leitura para consulta de dados processuais do TJSC. Porta **8081**. Não conhece LLM, MCP nem Spring AI — responsabilidade única: dados.

## Stack

- Java 25 / Spring Boot 4.1.1
- JDBC com `NamedParameterJdbcTemplate` (sem JPA)
- SQLite via `sqlite-jdbc:3.49.1.0`
- HikariCP com `maximum-pool-size: 1` (SQLite é single-writer por design)
- Swagger UI: `http://localhost:8081/swagger-ui.html`

## Arquitetura hexagonal

```
ProcessoController (adapter/in/web)
    └── UseCases (application/port/in)
         └── Services (application/service)
              └── ProcessoRepository (application/port/out)
                   └── SQLiteProcessoRepository (adapter/out/sqlite)
```

Cada endpoint tem seu próprio `UseCase` interface, seu `Service` e seu método no `SQLiteProcessoRepository`. Exemplo: `BuscarProcessoUseCase → ProcessoService → SQLiteProcessoRepository.buscarPorNumero()`.

## Proteção somente leitura — duas camadas

**Camada 1 — Hikari connection init:**
```yaml
hikari:
  connection-init-sql: "PRAGMA query_only = ON"
```
Toda conexão do pool executa `PRAGMA query_only = ON` ao ser criada. Qualquer tentativa de escrita no JDBC principal é rejeitada pelo SQLite.

**Camada 2 — `ReadOnlyDataSourceConfig` para o SqlController:**
```java
SQLiteConfig config = new SQLiteConfig();
config.setOpenMode(SQLiteOpenMode.READONLY);
config.setReadOnly(true);
```
O `SqlController` recebe um `JdbcTemplate` separado (`readOnlyJdbcTemplate`) com `SQLiteOpenMode.READONLY` — nível de sistema operacional, bloqueia escrita mesmo que a query passe pela validação de keyword.

## Normalização de número de processo

`NumeroProcessoNormalizer` aceita 3 formatos e normaliza para CNJ:

| Formato de entrada | Exemplo |
|---|---|
| CNJ já normalizado | `0000001-02.2019.8.24.0000` |
| 20 dígitos contíguos | `00000010220198999018` |
| Com prefixo textual | `Processo 0000001-02.2019.8.24.0000` |

Regex de validação: `^\d{7}-\d{2}\.\d{4}\.\d\.\d{2}\.\d{4}$`

Segmentação dos 20 dígitos: `NNNNNNN(7) DD(2) AAAA(4) J(1) TT(2) OOOO(4)`

Lança `NumeroProcessoInvalidoException` (HTTP 400) se nenhum formato for reconhecido.

## Endpoints

| Método | Path | Descrição |
|---|---|---|
| GET | `/api/v1/processos/{numero}` | Dados básicos do processo |
| GET | `/api/v1/processos/{numero}/contexto` | Tudo em uma chamada (otimizado para MCP) |
| GET | `/api/v1/processos/{numero}/partes` | Lista de partes com polo |
| GET | `/api/v1/processos/{numero}/advogados` | Advogados extraídos de `documento.autor LIKE '%OAB/%'` |
| GET | `/api/v1/processos/{numero}/magistrados` | Magistrado responsável |
| GET | `/api/v1/processos/{numero}/movimentacoes?page&size` | Movimentações paginadas (DESC por data) |
| GET | `/api/v1/processos/{numero}/documentos?page&size` | Documentos sem texto, paginados |
| GET | `/api/v1/processos/{numero}/documentos/{id}` | Texto integral de um documento específico |
| GET | `/api/v1/processos/{numero}/documentos/texto?tipo&page&size` | Documentos com texto, filtro por tipo |
| GET | `/api/v1/processos/{numero}/sentenca` | Sentença principal com texto (prioridade: Sentença > Homologatória) |
| GET | `/api/v1/processos/{numero}/peticao` | Petição inicial com texto (prioridade: Inicial > Emenda) |
| GET | `/api/v1/processos/{numero}/decisao` | Metadados de decisão sem texto (prioridade: Sentença > Homologatória > Interlocutória > Saneamento) |
| GET | `/api/v1/processos/pesquisar?parte&classe&comarca&situacao&page&size` | Pesquisa por filtros combinados (AND, busca parcial) |
| POST | `/api/v1/sql` | SELECT direto no SQLite (uso exclusivo pelo MCP) |

## SqlController — validação de SELECT

`SqlController` (package-private, não exposto no Swagger público) valida:
1. Query deve começar com `SELECT`
2. Remove `;` trailing; rejeita multi-statement (`;` no meio)
3. Regex contra keywords bloqueadas: `INSERT UPDATE DELETE DROP CREATE ALTER TRUNCATE REPLACE ATTACH DETACH PRAGMA VACUUM REINDEX ANALYZE USE`
4. Adiciona `LIMIT 100` se não houver `LIMIT` explícito

## Política de respostas

- Processo não encontrado → HTTP 404 + body `{found: false}`
- Número inválido → HTTP 400
- Processo existe mas não tem o recurso solicitado → HTTP 200 + `{found: false, message: "..."}` (não é erro)
- Erros internos → HTTP 500 sem stack trace

## Testes

53 testes de integração em `SQLiteProcessoRepositoryTest` e `ProcessoControllerTest` usando o `desafio.sqlite` real (não mock).

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SQLITE_DATABASE_PATH` | `../desafio.sqlite` | Caminho do banco SQLite |
