---
type: Reference
title: Catálogo das 14 ferramentas MCP
description: Todas as ferramentas MCP disponíveis no process-mcp-server com descrição, parâmetros e quando usar.
tags: [mcp, tools, spring-ai, process-mcp-server]
sources:
  - resource: "backend/process-mcp-server/src/main/java/br/jus/tjsc/ai/mcp/ProcessoTools.java"
    title: "ProcessoTools.java"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Catálogo das 14 Ferramentas MCP

Todas as ferramentas são expostas via `POST http://localhost:8082/mcp` (MCP Streamable HTTP, protocolo 2024-11-05).

## Consulta por processo

### `buscar_contexto_completo`

**Parâmetro**: `numero` (string, CNJ ou 20 dígitos)  
**Retorna**: processo + vara + partes + advogados + magistrado + movimentações + sentença com texto + petição inicial com texto  
**Quando usar**: ponto de entrada padrão para qualquer pergunta abrangente — "me explique o processo", "o que aconteceu?", "qual foi o resultado?"

---

### `buscar_processo`

**Parâmetro**: `numero`  
**Retorna**: classe, assunto, comarca, vara, `dt_aut`, `data_sentenca`, `valor_causa`, `situacao`  
**Quando usar**: só quando precisar de dados básicos sem partes, movimentações ou documentos  
**NÃO inclui**: movimentações, partes, advogados, documentos

---

### `buscar_partes`

**Parâmetro**: `numero`  
**Retorna**: lista de partes com nome, tipo, documento (CPF/CNPJ), polo (ativo/passivo)  
**Quando usar**: "quem são as partes?", "quem é o réu?", "quem é o autor?"

---

### `buscar_advogados`

**Parâmetro**: `numero`  
**Retorna**: advogados extraídos de `documento.autor LIKE '%OAB/%'` com nome e número OAB  
**Quando usar**: "qual é o advogado?", "quem representa as partes?", "número da OAB"  
**Nota**: não é tabela separada — derivado dos documentos

---

### `buscar_magistrado`

**Parâmetro**: `numero`  
**Retorna**: magistrado responsável com nome e situação  
**Quando usar**: "quem é o juiz?", "qual magistrado?", "quem assinou a sentença?"

---

### `buscar_movimentacoes`

**Parâmetro**: `numero`  
**Retorna**: 50 movimentações mais recentes, ordenadas por `data_mov DESC`  
**Quando usar**: DEVE ser chamada SEMPRE que o usuário perguntar sobre movimentações, andamento, histórico — **mesmo que `buscar_contexto_completo` já tenha sido chamada**  
**Nota**: é a ÚNICA fonte de movimentações processuais

---

### `buscar_documentos`

**Parâmetro**: `numero`  
**Retorna**: 50 documentos mais recentes com tipo, autor e data — **sem texto integral**  
**Quando usar**: para saber quais documentos existem antes de solicitar o texto integral

---

### `buscar_documento_por_id`

**Parâmetros**: `numero`, `documentoId` (Long)  
**Retorna**: texto integral de um documento específico pelo ID  
**Quando usar**: quando souber o ID (obtido via `buscar_documentos`)

---

### `buscar_documentos_com_texto`

**Parâmetros**: `numero`, `tipo` (string — envie vazio para todos os tipos)  
**Retorna**: até 5 documentos com texto integral, filtrados por tipo  
**Tipos válidos**: `'Despacho'`, `'Certidão'`, `'Petição Inicial'`, `'Ata de Audiência'`, `'Contestação'`, `'Sentença'`, `'Réplica'`, `'Decisão de Saneamento'`, `'Decisão Interlocutória'`, `'Manifestação sobre Provas'`, `'Petição de Acordo'`, `'Sentença Homologatória de Acordo'`, `'Parecer do Ministério Público'`, `'Exceção de Pré-Executividade'`, `'Emenda à Inicial'`, `'Embargos à Execução Fiscal'`, `'Embargos de Declaração'`, `'Apelação'`, `'Contrarrazões de Apelação'`  
**Quando usar**: perguntas sobre despachos, contestação, atas de audiência  
**NÃO use junto com** `buscar_sentenca`/`buscar_peticao_inicial` para os mesmos tipos

---

### `buscar_sentenca`

**Parâmetro**: `numero`  
**Retorna**: sentença principal com texto integral (prioridade: Sentença > Sentença Homologatória)  
**Quando usar**: "qual foi a sentença?", "o que diz a sentença?", "o processo foi julgado?", "qual a decisão final?"

---

### `buscar_peticao_inicial`

**Parâmetro**: `numero`  
**Retorna**: petição inicial com texto integral (prioridade: Petição Inicial > Emenda à Inicial)  
**Quando usar**: "qual é o pedido?", "o que pede o autor?", "quais são os fatos narrados?", "como o processo foi iniciado?"

---

### `buscar_decisao`

**Parâmetro**: `numero`  
**Retorna**: tipo e data da decisão principal — **sem texto integral**  
**Prioridade**: Sentença > Sentença Homologatória > Decisão Interlocutória > Decisão de Saneamento  
**Quando usar**: verificar o tipo e data da decisão sem precisar do texto  
**NÃO inclui**: despachos (para despachos usar `buscar_documentos_com_texto` com `tipo='Despacho'`)

---

## Pesquisa e análise

### `pesquisar_processos`

**Parâmetros**: `parte`, `classe`, `comarca`, `situacao` (todos strings — envie vazio se não souber)  
**Retorna**: até 20 processos com busca parcial por nome de parte e situação  
**Quando usar**: "processos do João Silva", "processos em Florianópolis", "processos julgados de Procedimento Comum"

---

### `executar_sql`

**Parâmetro**: `sql` (SELECT válido em SQLite)  
**Retorna**: até 100 linhas (LIMIT adicionado automaticamente se ausente)  
**Quando usar**: perguntas analíticas que as ferramentas específicas não cobrem — contagens, rankings, médias, distribuições, comparações entre múltiplos processos

**Regras críticas**:
1. `processo.comarca` já tem o nome — **NÃO** faça JOIN com `unidade` só para comarca
2. `situacao` tem casing inconsistente — **sempre** `LOWER(situacao) LIKE '%valor%'`
3. Para magistrado: `JOIN magistrado m ON p.magistrado_id = m.id WHERE LOWER(m.nome) LIKE '%nome%'`
4. `polo` em `processo_parte`: `'ativo'` ou `'passivo'` (minúsculo)
5. Apenas SELECT é permitido — INSERT/UPDATE/DELETE/DROP são bloqueados em 3 camadas

# Examples

```sql
-- Comarca com mais processos
SELECT comarca, COUNT(*) as total FROM processo GROUP BY comarca ORDER BY total DESC LIMIT 5

-- Magistrados com mais processos julgados
SELECT m.nome, COUNT(*) as total FROM processo p
JOIN magistrado m ON p.magistrado_id = m.id
WHERE LOWER(p.situacao) LIKE '%julgado%'
GROUP BY m.nome ORDER BY total DESC LIMIT 10

-- Distribuição por classe processual
SELECT classe, COUNT(*) as total FROM processo GROUP BY classe ORDER BY total DESC
```
