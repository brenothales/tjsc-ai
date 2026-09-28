---
type: Decision
title: SQLite somente leitura — duas camadas independentes
description: Como o process-data-service garante que nenhuma escrita seja possível no SQLite, com duas camadas independentes de proteção.
tags: [sqlite, segurança, somente-leitura, jdbc]
sources:
  - resource: "backend/process-data-service/src/main/resources/application.yml"
    title: "application.yml — hikari connection-init-sql"
  - resource: "backend/process-data-service/src/main/java/br/jus/tjsc/ai/process/config/ReadOnlyDataSourceConfig.java"
    title: "ReadOnlyDataSourceConfig.java"
  - resource: "backend/process-data-service/src/main/java/br/jus/tjsc/ai/process/adapter/in/web/SqlController.java"
    title: "SqlController.java"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Decisão: SQLite Somente Leitura em Duas Camadas

## Contexto

O `process-data-service` expõe um endpoint `POST /api/v1/sql` que executa SELECT diretamente no SQLite. Um atacante poderia tentar contornar a validação de keywords para executar uma query de escrita. Duas camadas independentes garantem que isso seja impossível mesmo que a validação seja bypassada.

## Camada 1 — PRAGMA query_only (Hikari)

```yaml
# application.yml
spring:
  datasource:
    hikari:
      connection-init-sql: "PRAGMA query_only = ON"
```

Executado em **toda conexão** ao ser criada pelo pool. `PRAGMA query_only = ON` instrui o SQLite a rejeitar qualquer operação de escrita naquela conexão, mesmo que a query seja sintaticamente válida. Esta é a camada default usada pelo `NamedParameterJdbcTemplate` principal.

**Limitação**: é uma configuração de sessão SQL, não de arquivo. Teoricamente poderia ser revertida por outro `PRAGMA query_only = OFF` em multi-statement.

## Camada 2 — SQLiteOpenMode.READONLY (SqlController)

```java
// ReadOnlyDataSourceConfig.java
SQLiteConfig config = new SQLiteConfig();
config.setOpenMode(SQLiteOpenMode.READONLY);  // nível de sistema operacional
config.setReadOnly(true);

SQLiteDataSource dataSource = new SQLiteDataSource(config);
```

O `SqlController` (que executa SELECTs arbitrários do MCP) recebe um `JdbcTemplate` separado (`@Qualifier("readOnlyJdbcTemplate")`) criado com `SQLiteOpenMode.READONLY`. Este modo é aplicado no nível do driver JDBC/sistema operacional — o arquivo SQLite é aberto com flag de read-only do SO. Nenhum statement de escrita é possível, independentemente do que a query contenha.

**Esta é a camada mais forte**: mesmo que o código do `SqlController` tivesse uma vulnerabilidade que permitisse passar um `UPDATE` pelo filtro de keywords, o SQLite rejeitaria no nível do SO.

## Camada bônus — validação de keywords

`SqlController` ainda valida a query antes de executar:
1. Deve começar com `SELECT`
2. Multi-statement bloqueado (`;` no meio)
3. Keywords bloqueadas por regex: `INSERT UPDATE DELETE DROP CREATE ALTER TRUNCATE REPLACE ATTACH DETACH PRAGMA VACUUM REINDEX ANALYZE USE`

Esta camada não é a principal barreira de segurança (pode ter falsos negativos), mas cria fricção adicional e fornece logs de auditoria quando uma tentativa é detectada.

## Por que duas camadas e não uma?

Defense in depth. As duas camadas são independentes — uma falha em uma não compromete a outra:
- Camada 1 (PRAGMA) pode ser contornada por multi-statement antes da validação — por isso existe a validação de `;`
- Camada 2 (READONLY) não pode ser contornada por nenhuma query SQL — é enforced pelo SO
- Camada 3 (validação) complementa com auditoria e fail-fast antes de chegar ao SQLite
