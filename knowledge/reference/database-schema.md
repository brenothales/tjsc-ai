---
type: Reference
title: Schema do banco SQLite — desafio.sqlite
description: Estrutura das 7 tabelas do banco sintético TJSC com volumes, índices e entidades derivadas.
tags: [sqlite, schema, banco-de-dados, tjsc]
sources:
  - resource: "backend/process-data-service/DATA_MODEL.md"
    title: "DATA_MODEL.md"
  - resource: "backend/process-mcp-server/src/main/java/br/jus/tjsc/ai/mcp/ProcessoTools.java"
    title: "ProcessoTools.java — descrição do schema em executar_sql"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Schema do banco SQLite — desafio.sqlite

Banco de dados sintético TJSC com dados fictícios gerados para o desafio. Somente leitura.

## Tabelas

### processo — 10.000 registros

```sql
processo(
  id           INTEGER PRIMARY KEY,
  numero       TEXT UNIQUE,        -- formato CNJ: NNNNNNN-DD.AAAA.J.TT.OOOO
  classe       TEXT,               -- "Procedimento Comum Cível", "Execução Fiscal", etc.
  assunto      TEXT,
  comarca      TEXT,               -- nome direto (não FK para unidade)
  unidade_id   INTEGER,            -- FK → unidade.id
  magistrado_id INTEGER,           -- FK → magistrado.id
  dt_aut       TEXT,               -- data de autuação (string)
  data_sentenca TEXT,              -- null se não julgado
  valor_causa  REAL,
  situacao     TEXT                -- casing inconsistente — sempre usar LOWER()
)
```

**Atenção — `situacao`**: valores como `julgado`, `Julgado`, `JULGADO`, `em andamento`, `Em Andamento` coexistem no banco. **Sempre** filtre com `WHERE LOWER(situacao) LIKE '%valor%'`.

**Atenção — `comarca`**: já é o nome da comarca. **NÃO** fazer JOIN com `unidade` só para obter o nome da comarca.

### parte — ~14.966 registros

```sql
parte(
  id            INTEGER PRIMARY KEY,
  nome          TEXT,
  tipo          TEXT,       -- "autor", "réu" (ou variações)
  documento     TEXT,       -- CPF/CNPJ — NUNCA expor ao usuário
  data_cadastro TEXT
)
```

### processo_parte — ~25.086 registros

```sql
processo_parte(
  id          INTEGER PRIMARY KEY,
  processo_id INTEGER,  -- FK → processo.id
  parte_id    INTEGER,  -- FK → parte.id
  polo        TEXT      -- 'ativo' ou 'passivo' (minúsculo)
)
-- Índices: idx_pp_processo(processo_id), idx_pp_parte(parte_id)
```

### magistrado — 200 registros

```sql
magistrado(
  id         INTEGER PRIMARY KEY,
  nome       TEXT,
  unidade_id INTEGER,  -- FK → unidade.id
  data_inicio TEXT,
  situacao   TEXT      -- "ativo", "aposentado", etc.
)
```

### documento — ~25.582 registros (média 6,9 por processo, max 19)

```sql
documento(
  id          INTEGER PRIMARY KEY,
  processo_id INTEGER,  -- FK → processo.id
  tipo        TEXT,     -- ver tipos válidos abaixo
  autor       TEXT,     -- advogado (inclui "OAB/SC XX.XXX") ou magistrado
  dt_juntada  TEXT,
  num_paginas INTEGER,
  texto       TEXT      -- texto integral — NÃO exposto em listagens
)
-- Índice: idx_doc_processo(processo_id)
```

**Tipos de documento válidos:**
`Petição Inicial`, `Despacho`, `Contestação`, `Réplica`, `Certidão`, `Ata de Audiência`,
`Decisão Interlocutória`, `Decisão de Saneamento`, `Sentença`, `Sentença Homologatória de Acordo`,
`Parecer do Ministério Público`, `Apelação`, `Contrarrazões de Apelação`,
`Embargos de Declaração`, `Embargos à Execução Fiscal`,
`Exceção de Pré-Executividade`, `Emenda à Inicial`,
`Manifestação sobre Provas`, `Petição de Acordo`

### movimentacao — ~134.046 registros (média 13 por processo, max 25)

```sql
movimentacao(
  id          INTEGER PRIMARY KEY,
  processo_id INTEGER,  -- sem FK constraint, mas consistente
  codigo      INTEGER,  -- código CNJ da movimentação
  descricao   TEXT,
  data_mov    TEXT
)
-- Índice: idx_mov_processo(processo_id)
```

### unidade — 118 registros

```sql
unidade(
  id      INTEGER PRIMARY KEY,
  comarca TEXT,
  vara    TEXT,
  dt_ini  TEXT,
  status  TEXT  -- "ativa", "inativa"
)
```

## Entidades derivadas (sem tabela própria)

**Advogado**: extraído de `documento.autor LIKE '%OAB/%'`  
Regex de parse: `^(.+?)\s+[—–-]\s+OAB/([A-Z]{2}\s+[\d.]+)$`  
Exemplo: `"Dr. João Silva — OAB/SC 12.345"` → nome=`"Dr. João Silva"`, oab=`"OAB/SC 12.345"`

**Decisão principal**: documento com tipo em `{Sentença, Sentença Homologatória de Acordo, Decisão Interlocutória, Decisão de Saneamento}`, ordenado por prioridade e `dt_juntada DESC LIMIT 1`

**Petição inicial**: documento com tipo em `{Petição Inicial, Emenda à Inicial}`, prioridade Inicial > Emenda, `dt_juntada ASC LIMIT 1`

## Exemplos de SQL para executar_sql

```sql
-- Comarcas com mais processos
SELECT comarca, COUNT(*) as total
FROM processo
GROUP BY comarca
ORDER BY total DESC
LIMIT 10

-- Processos de um magistrado (use LIKE para variações de nome)
SELECT COUNT(*) FROM processo p
JOIN magistrado m ON p.magistrado_id = m.id
WHERE LOWER(m.nome) LIKE '%nome do magistrado%'

-- Processos em andamento por classe
SELECT classe, COUNT(*) as total
FROM processo
WHERE LOWER(situacao) LIKE '%andamento%'
GROUP BY classe
ORDER BY total DESC

-- Partes com mais processos no polo ativo
SELECT pa.nome, COUNT(DISTINCT pp.processo_id) as total
FROM parte pa
JOIN processo_parte pp ON pa.id = pp.parte_id
WHERE pp.polo = 'ativo'
GROUP BY pa.nome
ORDER BY total DESC
LIMIT 10
```
