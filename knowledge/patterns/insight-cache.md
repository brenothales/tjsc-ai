---
type: Architecture Pattern
title: Insight Cache Redis
description: Cache condicional de respostas para processos finalizados — evita chamadas ao LLM para o mesmo processo quando a situação não muda.
tags: [redis, cache, spring-ai, performance]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/infrastructure/InsightCacheService.java"
    title: "InsightCacheService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/infrastructure/ProcessStatusChecker.java"
    title: "ProcessStatusChecker.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/application/ChatServiceImpl.java"
    title: "ChatServiceImpl.java"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Insight Cache Redis

Cache de respostas do agente para processos que não estão "em andamento". Processos julgados/arquivados não mudam — gerar a mesma resposta novamente desperdiça tokens e aumenta latência.

## Fluxo

```
ChatServiceImpl.stream(request)
    │
    ├─ insightKey presente?
    │     Não → agentService.chat() direto (sem cache)
    │     Sim → streamWithCache(request, conversationId)
    │
    └─ streamWithCache():
          │
          ├─ bypassCache=false E Redis.get("insight:{key}") hit?
          │     → emite sentinela [INSIGHT:{fromCache:true, insightKey:"..."}]
          │     → emite conteúdo cacheado
          │     → emite [DONE]
          │     ZERO chamadas ao LLM
          │
          └─ miss OU bypassCache=true:
                → evict(key) se bypassCache
                → emite sentinela [INSIGHT:{fromCache:false, insightKey:"..."}]
                → agentService.chat() com acumulador em StringBuilder
                → doOnComplete: ProcessStatusChecker.canCache(numero)?
                      Sim (processo não "em andamento") → Redis.set("insight:{key}", content, TTL=1h)
                      Não → não cacheia
```

## insightKey

Formato definido pelo cliente (frontend): tipicamente `{prefixo}:{numero_processo}`.

`ChatServiceImpl` extrai o número do processo da chave: `key.substring(key.indexOf(':') + 1)` para passar ao `ProcessStatusChecker`.

## ProcessStatusChecker

Faz `GET /api/v1/processos/{numero}` no `process-data-service` e verifica se a resposta contém `"andamento"` (case-insensitive). Se o processo estiver em andamento → não cacheia. Em caso de erro → não cacheia (fail-safe).

## InsightCacheService

```java
PREFIX = "insight:"
TTL    = Duration.ofHours(1)   // 1 hora

get(key)     → Redis.opsForValue().get("insight:" + key)
set(key, v)  → Redis.opsForValue().set("insight:" + key, v, TTL)
evict(key)   → Redis.delete("insight:" + key)
```

## Sentinela no stream

O frontend detecta a linha `[INSIGHT:{...}]` antes dos chunks de conteúdo e usa `fromCache` para exibir um indicador visual na mensagem:
- `fromCache: true` → mostra ícone de cache + botão de refresh
- `fromCache: false` → resposta nova gerada pelo LLM

## refreshInsight (frontend)

`ChatService.refreshInsight(messageId)` reenvia a mensagem com `bypassCache: true`, forçando regeneração e nova gravação no Redis.
