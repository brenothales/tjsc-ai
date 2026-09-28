# Melhorias — TJSC AI Platform

Registro de melhorias identificadas para evolução da plataforma em direção a um ambiente de produção no tribunal.

---

## 1. Observabilidade

**Estado atual:** logs no stdout, métricas básicas via Actuator (`/actuator/health`, `/actuator/metrics`).

**O que adicionar:**

- **Distributed tracing** — instrumentar `process-agent`, `process-mcp-server` e `process-data-service` com OpenTelemetry e exportar para Jaeger ou Zipkin. Permite rastrear uma pergunta do usuário do SSE até a chamada SQL, passando pelo MCP.
- **Métricas estruturadas** — Micrometer já está presente; adicionar um contêiner Prometheus + Grafana no `docker-compose.yaml` e criar dashboards para: latência P95 por ferramenta MCP, tokens consumidos por sessão, taxa de rejeição dos guardrails, duração do minuta loop por etapa.
- **Logs estruturados (JSON)** — substituir o padrão de log texto por JSON (Logback + `logstash-logback-encoder`) e centralizar em um stack ELK ou Grafana Loki. Facilita correlacionar `conversationId` + `traceId` em uma única query.
- **Alertas** — regras no Grafana para latência acima de SLA ou taxa de erro de tool call acima de threshold.

---

## 2. Evaluation e Curadoria de Respostas

**Estado atual:** nenhuma avaliação automatizada; qualidade depende de testes manuais.

**O que adicionar:**

- **Dataset golden** — criar um conjunto de pares `(pergunta, resposta esperada)` baseado em processos reais do banco sintético. Versionar junto ao repositório em `evaluation/golden-dataset.json`.
- **LLM-as-Judge** — usar um segundo modelo (GPT-4o ou Claude) para avaliar automaticamente: fidelidade aos dados retornados pelas ferramentas, completude da resposta, ausência de alucinação. Integrar no CI como etapa `mvn verify -Pevaluation`.
- **Métricas RAGAS-like** — `faithfulness` (resposta sustentada pelos dados), `answer_relevancy`, `context_recall`. Calcular por sessão e por versão de prompt.
- **Feedback humano** — botões de 👍 / 👎 no frontend armazenando `{ conversationId, messageIndex, rating, comment }` no MongoDB. Usar como sinal de curadoria incremental.
- **Prompt versioning** — versionar o system prompt no Git com tag semântica e registrar qual versão gerou cada resposta no `ConversationMetadata`.

---

## 3. Autenticação, Autorização e Processo Sigiloso

**Estado atual:** sem autenticação; qualquer requisição chega ao agente.

### 3.1 Autenticação

- **OAuth2 / OIDC** — integrar com um Identity Provider (Keycloak, ou o IdP já usado pelo tribunal). Spring Security já é dependência transitiva do Spring Boot; adicionar `spring-boot-starter-oauth2-resource-server` nos três serviços.
- **JWT** — propagar o token do frontend → `process-agent` → `process-mcp-server` → `process-data-service` via header `Authorization: Bearer`. Cada serviço valida a assinatura localmente sem round-trip ao IdP.

### 3.2 Perfis de Acesso (RBAC)

| Perfil | Pode perguntar | Pode gerar minuta | Vê processos sigilosos |
|---|---|---|---|
| `ADVOGADO` | Apenas processos onde é habilitado | Não | Não |
| `MAGISTRADO` | Todos os processos da vara | Sim | Sim |
| `SERVIDOR` | Processos públicos | Não | Não |
| `GESTOR_TI` | Sem acesso a dados processuais | — | Não |

- Implementar via `@PreAuthorize` no `ChatController` e `MinutaController`.
- O `process-data-service` recebe o perfil no JWT e aplica filtros SQL adicionais (ex.: `WHERE segredo_justica = false` para não-magistrados).

### 3.3 Processo Sigiloso (Segredo de Justiça)

- Adicionar coluna `segredo_justica BOOLEAN` na camada de dados (já presente no schema? verificar).
- `process-data-service`: filtrar automaticamente processos sigilosos para perfis sem permissão.
- `process-mcp-server`: propagar o contexto de autorização nas chamadas ao data service.
- `process-agent`: guardrail adicional que impede o LLM de expor dados sigilosos mesmo que a ferramenta os retorne (política no `OutputGuardrailAdvisor`).

---

## 4. Separação de Serviços: Minuta e Conversation

**Estado atual:** `MinutaService`, `MinutaLoopService`, `ConversationService` e `AgentService` convivem dentro do `process-agent`. O pacote já está bem separado (`chat.*` vs `minuta.*`), mas compartilham o mesmo processo JVM, porta e ciclo de deploy.

### 4.1 `process-minuta-service` (novo serviço)

Extrair o pacote `minuta.*` para um serviço independente:

```
process-agent       → orquestração de chat, guardrails, SSE
process-minuta-service  → MinutaLoopService, ColetaDadosService, RedacaoService, MinutaRepository
```

- Comunicação via HTTP REST (ou evento assíncrono — ver item 8).
- Permite escalar e fazer deploy da geração de minuta independentemente do chat.
- Isola o loop multi-etapa (que consome mais tokens e tempo) do caminho crítico de resposta ao usuário.

### 4.2 `process-conversation-service` (opcional, fase posterior)

Extrair `ConversationService` + `ConversationMetadataRepository` para um serviço de histórico:

- Permite que outros clientes (portal web do tribunal, app mobile) consumam o histórico de conversas sem passar pelo agente.
- Expõe API paginada de conversas por `userId` com filtros por processo, data e perfil.

---

## 5. RAG sobre Documentos Processuais

**Estado atual:** busca por texto integral via `LIKE` no SQLite.

- Gerar embeddings dos documentos processuais (OpenAI `text-embedding-3-small` ou modelo local) e armazená-los em um vector store (pgvector, Weaviate ou o próprio MongoDB Atlas Vector Search).
- Substituir a ferramenta `buscarDocumentosPorProcesso` por recuperação semântica: o agente recebe os chunks mais relevantes para a pergunta, não o documento inteiro.
- Reduz consumo de tokens e melhora precisão para documentos longos (petições, laudos).

---

## 6. Testes Automatizados

**Estado atual:** sem testes automatizados identificados.

- **Testes de contrato MCP** — verificar que cada `@McpTool` retorna o schema esperado. Usar `spring-ai-test` ou WireMock.
- **Testes de guardrail** — suite de inputs maliciosos que devem ser bloqueados; inputs legítimos que não devem ser bloqueados (evitar falsos positivos).
- **Testes de integração do agente** — usar `MockMvc` + modelo stubado para validar o fluxo completo `ChatController → AgentService → MCP → data`.
- **Testes de regressão de prompt** — executar o dataset golden a cada PR e bloquear merge se `faithfulness < 0.85`.

---

## 7. Comunicação Assíncrona para o Minuta Loop

**Estado atual:** `MinutaLoopService` roda de forma síncrona no `process-agent`; o frontend acompanha via SSE de status.

- Publicar um evento `MinutaSolicitada` em um broker (RabbitMQ ou Kafka) quando o usuário solicita a geração.
- `process-minuta-service` consome o evento, executa o loop e publica `MinutaConcluida`.
- `process-agent` consome `MinutaConcluida` e notifica o frontend via SSE.
- Benefícios: resiliência a falhas do agente durante a geração, retry automático, fila de geração por magistrado.
- **Temporal** — alternativa ao broker para orquestrar o Minuta Loop como um workflow durável: cada etapa (`ColetaDados → Redacao → Revisao`) vira uma Activity, o estado de execução fica persistido no Temporal Server e o loop retoma exatamente de onde parou em caso de falha ou restart do contêiner — sem reprocessar etapas já concluídas. Especialmente útil quando o loop cresce em número de etapas ou passa a envolver aprovação humana (magistrado revisa rascunho antes da versão final).

---

## 8. Audit Trail (LGPD e Resolução CNJ)

**Estado atual:** sem registro de auditoria formal.

- Gravar cada consulta processual em uma coleção imutável `audit_log` no MongoDB: `{ userId, role, conversationId, processNumber, toolCalled, timestamp }`.
- Implementar como advisor (`AuditAdvisor`) no advisor chain, após o `InputGuardrailAdvisor`.
- Expor endpoint `/audit` restrito ao perfil `GESTOR_TI` com filtros por usuário, processo e período.
- Atende ao art. 37 da LGPD (rastreabilidade de acesso a dados pessoais sensíveis).

---

## 9. CI/CD

**Estado atual:** build manual via `docker compose up --build`.

- Pipeline GitHub Actions (ou GitLab CI) com etapas: `build → test → evaluation → docker build → deploy`.
- Publicar imagens no GitHub Container Registry com tag baseada na versão do `package.json` (gerada pelo `release-it`).
- Deploy automático em ambiente de homologação a cada merge na `main`; produção via aprovação manual.

---

## Priorização Sugerida

| # | Melhoria | Impacto | Esforço | Prioridade |
|---|---|---|---|---|
| 3 | Autenticação e autorização | Alto | Médio | Alta |
| 8 | Audit trail | Alto | Baixo | Alta |
| 1 | Observabilidade | Alto | Médio | Alta |
| 4 | Separação minuta/conversation | Médio | Médio | Média |
| 2 | Evaluation e curadoria | Médio | Alto | Média |
| 6 | Testes automatizados | Alto | Alto | Média |
| 9 | CI/CD | Médio | Médio | Média |
| 5 | RAG sobre documentos | Alto | Alto | Baixa |
| 7 | Comunicação assíncrona | Médio | Alto | Baixa |
