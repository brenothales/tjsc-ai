# DECISOES.md — Decisões Arquiteturais

---

## Por que este serviço existe?

O `process-data-service` separa a responsabilidade de acesso aos dados processuais do protocolo MCP.

Sem essa separação, o `process-mcp-server` precisaria conhecer os detalhes do SQLite, as queries SQL, o schema das tabelas e as regras de normalização do número do processo. Isso acoplaria IA e dados, tornando o sistema frágil: uma mudança de banco impactaria o MCP server.

Com a separação, o MCP server consome uma API HTTP estável e orientada ao domínio. O contrato não muda quando o banco muda.

---

## Por que SQLite?

É o banco fornecido pelo desafio. O serviço está preparado para substituição: a implementação SQLite fica isolada em `adapter/out/sqlite`. Trocar para PostgreSQL significa criar um novo adapter sem alterar o domínio, os services ou os controllers.

---

## Por que JDBC e não JPA?

**O domínio é predominantemente leitura.** As queries têm joins específicos, projeções parciais, paginação e ordenação determinística — exatamente o que o JDBC expressa diretamente.

JPA traria:
- Overhead de mapeamento objeto-relacional desnecessário
- Risco de lazy loading e N+1 em entidades relacionadas
- Dificuldade de expressar `LIMIT/OFFSET` nativamente em SQLite
- Abstração que obscurece queries críticas para performance

JDBC com `NamedParameterJdbcTemplate` oferece:
- Queries explícitas, auditáveis, otimizáveis
- Parâmetros nomeados (sem risco de SQL injection)
- Zero overhead de estado gerenciado
- Compatibilidade total com SQLite

---

## Por que não Text-to-SQL?

SQL gerado por LLM:

1. **Não é determinístico** — a mesma pergunta pode gerar queries diferentes
2. **Aumenta superfície de ataque** — o LLM pode ser induzido a gerar queries prejudiciais
3. **Não garante somente leitura** — um LLM poderia gerar DELETE, UPDATE ou DROP
4. **Vaza schema interno** — o LLM precisa conhecer as tabelas para gerar SQL
5. **Difícil de auditar** — não há como registrar e revisar cada query gerada

Neste serviço, **todas as queries são pré-definidas no código**, revisadas por humanos e executadas com parâmetros. Isso é auditável, previsível e seguro.

---

## Como o serviço protege o banco?

### Somente leitura
A conexão usa `PRAGMA query_only = ON` configurado no `connection-init-sql`. Mesmo que um bug permita construir uma query de escrita, o SQLite a rejeitará.

### Queries pré-definidas
Nenhuma query é construída a partir de input externo. Todo SQL está no código-fonte do `SQLiteProcessoRepository`.

### Parâmetros nomeados
```java
MapSqlParameterSource params = new MapSqlParameterSource()
    .addValue("numero", numero);
```
Nunca concatenação de strings. O driver trata os valores como dados, nunca como SQL.

### Validação antes da query
O `NumeroProcessoNormalizer` valida o número do processo antes de qualquer acesso ao banco. Se a entrada for inválida (incluindo tentativas de SQL injection), uma exceção é lançada sem que nenhuma query seja executada.

### Sem API de SQL arbitrário
Não existe endpoint `POST /sql`, `GET /query?sql=...` ou método `executeSql(String)`.

---

## Por que separar MCP deste serviço?

Um serviço de dados não deve depender de IA. Isso permite que a mesma API seja consumida por:

- `process-mcp-server` (caso de uso atual)
- Interface REST direta
- Outros microsserviços
- Ferramentas de auditoria
- Sistemas legados via HTTP

Manter essa separação também facilita testes: este serviço pode ser testado completamente sem nenhum componente de IA.

---

## Por que não expor o campo `texto` dos documentos?

O campo `documento.texto` contém o texto integral de petições, sentenças e decisões — podendo ter dezenas de kilobytes por documento.

Expor `texto` na listagem de documentos:
- Sobrecarregaria respostas com dados raramente necessários
- Aumentaria latência sem benefício na maioria dos casos de uso
- Criaria risco de vazamento de dados sensíveis em logs

**Decisão:** `texto` não é exposto em nenhum endpoint da API pública. Se futuramente for necessário, um endpoint específico `GET /api/processos/{numero}/documentos/{id}/texto` pode ser criado.

---

## Política de HTTP status

| Situação                                         | HTTP |
|--------------------------------------------------|------|
| Processo não encontrado                          | 404  |
| Número de processo inválido                      | 400  |
| Parâmetro de paginação inválido (page < 0, size > 100) | 400  |
| Processo existe mas não tem determinada informação | 200 + `found: true, data: []` |
| Erro interno                                     | 500 (sem stack trace) |

---

## Por que paginação em movimentações e documentos?

- `movimentacao`: 134.046 registros, média de 13 por processo, máximo 25 — tecnicamente sem risco de N+1, mas a paginação prepara o serviço para crescimento.
- `documento`: 25.582 registros, média de ~7 por processo, máximo 19 — paginação implementada para consistência e proteção contra futuros volumes maiores.

Partes, advogados e magistrados **não são paginados** porque o volume por processo é pequeno e estável (tipicamente 2–5 partes, 1–3 advogados, 1 magistrado).

---

## Por que `size` máximo de 100?

Evita que um consumidor acidentalmente (ou maliciosamente) requisite volumes excessivos, como `size=999999`. O limite de 100 é suficiente para qualquer interface de usuário ou agente de IA e protege contra abusos acidentais.
