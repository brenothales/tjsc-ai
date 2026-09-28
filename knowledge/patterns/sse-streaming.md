---
type: Architecture Pattern
title: SSE Streaming end-to-end
description: Streaming de texto do LLM do backend (Flux<String>) até o frontend (ReadableStream) com sentinelas embutidas no fluxo de dados.
tags: [sse, reactor, angular, streaming, spring-webflux]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/api/ChatController.java"
    title: "ChatController.java"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/application/ChatServiceImpl.java"
    title: "ChatServiceImpl.java"
  - resource: "frontend/tjsc-ai/src/app/features/chat/shared/services/chat.api.service.ts"
    title: "chat.api.service.ts"
  - resource: "frontend/tjsc-ai/src/app/features/chat/shared/services/chat.service.ts"
    title: "chat.service.ts"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# SSE Streaming end-to-end

O streaming de texto percorre o caminho: LLM → Spring Reactor → HTTP SSE → Fetch API → TransformStream → Angular Signals.

## Backend — ChatController

```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<String> stream(@RequestBody ChatRequest request) {
    return chatService.stream(request);
}
```

Spring WebFlux mapeia `Flux<String>` diretamente para `text/event-stream`. Cada `String` emitida pelo Flux vira um evento SSE `data: {conteúdo}\n\n`.

## Sentinelas no fluxo

O `ChatServiceImpl` injeta sentinelas especiais no início do Flux para metadados:

```
[INSIGHT:{"fromCache":false,"insightKey":"..."}]   ← primeiro evento (quando insightKey presente)
chunk1
chunk2
...
[DONE]   ← último evento
```

As sentinelas são strings literais no fluxo de dados (não headers SSE separados), parsadas pelo frontend.

## Frontend — ChatApiService

```typescript
fetch(API.chatStream, { method: 'POST', headers: {...}, body: JSON.stringify(request), signal })
    .then(async (response) => {
        const reader = response.body
            .pipeThrough(new TextDecoderStream())
            .getReader();

        let buffer = '';
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += value;
            const events = buffer.split('\n\n');     // separador SSE
            buffer = events.pop() ?? '';              // mantém evento incompleto

            for (const event of events) {
                const data = event
                    .split('\n')
                    .filter(line => line.startsWith('data:'))
                    .map(line => line.slice(5))
                    .join('\n');

                if (data === '' || data.trim() === '[DONE]') continue;

                if (data.trim().startsWith('[INSIGHT:')) {
                    onInsightMeta?.(JSON.parse(data.trim().slice(9, -1)));
                    continue;
                }

                await writer.write(data);   // emite para ReadableStream
            }
        }
    })
```

O `TransformStream` isola o processamento do stream do loop de leitura. `AbortController.signal` permite cancelamento pelo usuário.

## Frontend — ChatService (consumidor)

```typescript
const reader = stream.getReader();
while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    this.ngZone.run(() => {
        this.messages.update(msgs => {
            const last = msgs[msgs.length - 1];
            return [...msgs.slice(0, -1), { ...last, content: last.content + value }];
        });
    });
}
```

`ngZone.run()` garante que a atualização do Signal acione a detecção de mudanças do Angular (necessário porque `reader.read()` resolve em microtask fora da zona Angular).

## Por que SSE e não WebSocket?

- SSE é unidirecional (servidor → cliente) — suficiente para streaming de resposta
- Funciona sobre HTTP/1.1 padrão, sem upgrade de protocolo
- Reconexão automática gerenciada pelo navegador
- Compatível com proxies/nginx sem configuração especial
- `Flux<String>` do Spring Reactor mapeia diretamente para `text/event-stream` sem código adicional

## Cancelamento

`ChatService.stopStreaming()` chama `abortController.abort()`. O `fetch` lança `AbortError` (capturado no `catch` do `ChatApiService` e ignorado silenciosamente). O `finally` do loop do `ChatService` limpa o estado normalmente.
