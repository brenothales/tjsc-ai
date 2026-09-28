---
type: Architecture Pattern
title: Minuta Loop (multi-step agent)
description: Geração de minutas de sentença em 3 etapas sequenciais com LLM especializado, retry automático e streaming de progresso via SSE.
tags: [spring-ai, minuta, multi-step, sse, reactor, mongodb]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/MinutaLoopService.java"
    title: "MinutaLoopService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/ColetaDadosService.java"
    title: "ColetaDadosService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/infrastructure/ai/RedacaoService.java"
    title: "RedacaoService.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/minuta/domain/VersaoTipo.java"
    title: "VersaoTipo.java"
  - resource: "backend/process-agent/src/main/resources/prompts/"
    title: "prompts/"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Minuta Loop — Multi-step Agent

Geração de minutas de sentença decomposta em etapas para produzir texto jurídico de qualidade superior a um único prompt.

## Por que multi-step?

Um único prompt "gere uma minuta completa" tende a produzir texto genérico, repetitivo e com omissões. A decomposição força o modelo a:
1. **Entender** os fatos antes de qualquer análise (coleta)
2. **Narrar** sem interpretar (relatório)
3. **Argumentar** com propósito específico (fundamentação + dispositivo)

Cada etapa tem um `ChatClient` dedicado (`minutaChatClient`) sem guardrails e sem memória de conversa — contexto limpo para cada chamada.

## Pipeline completo

```
MinutaLoopService.gerarStream(request)
    │  Schedulers.boundedElastic() — thread separada
    │
    ├─ [1] ColetaDadosService.coletar(numero)
    │      ChatClient + MCP tools (coleta-system.st + coleta-user.st)
    │      Chamadas obrigatórias em ordem:
    │        1. buscar_contexto_completo
    │        2. buscar_peticao_inicial
    │        3. buscar_documentos_com_texto tipo="Contestação"
    │        4. buscar_movimentacoes
    │        5. buscar_documentos_com_texto sem filtro
    │      Retorna: bloco estruturado com IDENTIFICAÇÃO, PARTES, FATOS, DEFESA, HISTÓRICO
    │
    ├─ [2] RedacaoService.relatorio(numero, dados)
    │      ChatClient sem tools (redacao-system.st + relatorio-user.st)
    │      Gera: narrativa factual neutra do processo
    │
    └─ [3] Para cada VersaoTipo (procedente / improcedente / parcialmente procedente):
           │
           ├─ RedacaoService.fundamentacao(numero, tipo, dados, relatorio)
           │   fundamentacao-user.st com {tipoLabel, tipoInstrucao, dados, relatorio}
           │
           └─ RedacaoService.dispositivo(numero, tipo, dados, relatorio, fundamentacao)
               dispositivo-user.st com cabeçalho formal TJSC, artigos CPC
               Inclui aviso: "⚠️ MINUTA — VERSÃO {tipoLabel} — SUJEITA À REVISÃO DO MAGISTRADO"
```

## VersaoTipo enum

```java
PROCEDENTE("procedente",
    "Elabore a fundamentação concluindo que os pedidos do autor devem ser ACOLHIDOS...")
IMPROCEDENTE("improcedente",
    "Elabore a fundamentação concluindo que os pedidos do autor devem ser REJEITADOS...")
PARCIALMENTE_PROCEDENTE("parcialmente procedente",
    "Elabore a fundamentação concluindo que PARTE dos pedidos é procedente...")
```

`VersaoTipo.fromLabel()` — se label desconhecido, fallback para `PARCIALMENTE_PROCEDENTE`.

## Retry automático

```java
String comRetry(Supplier<String> acao) {
    // MAX_RETRIES = 2
    // backoff linear: 1s após 1ª falha, 2s após 2ª falha
    // Lança RuntimeException("Falha após 2 tentativas") se todas falharem
}
```

## MinutaEventService — Reactor Sinks

`MinutaEventService` gerencia streams SSE com `Sinks.Many.unicast()`:
```java
Map<String, Sinks.Many<String>> streams = new ConcurrentHashMap<>();

criar(sessionId)  → Sinks.many().unicast().onBackpressureBuffer()
publicar(id, evt) → sink.tryEmitNext(evento)
concluir(id)      → sink.tryEmitComplete()
erro(id, t)       → sink.tryEmitError(t)
```

A `gerarStream()` cria o sink ANTES de iniciar o loop em background — garante que o cliente já está conectado quando os primeiros eventos chegarem.

## Persistência — MinutaDocument (MongoDB)

```
MinutaDocument {
  numero: String          — número CNJ do processo
  versaoMinuta: int       — 1, 2, 3... (incrementado por geração)
  versoes: List<MinutaVersao>
}

MinutaVersao {
  label: String           — "procedente" | "improcedente" | "parcialmente procedente"
  relatorio: String
  fundamentacao: String
  dispositivo: String
  steps: List<MinutaStep>
}
```

A versão da minuta (1, 2, 3...) é calculada como `MAX(versaoMinuta) + 1` ou `1` se não houver histórico.

## Endpoints

- `POST /api/v1/minuta/gerar` — inicia geração (SSE)
- `GET /api/v1/minuta/{numero}/historico` — lista versões passadas
- `GET /api/v1/minuta/{numero}/{versaoMinuta}` — detalhes de uma versão
