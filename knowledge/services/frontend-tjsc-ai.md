---
type: Service
title: frontend-tjsc-ai
description: Interface de chat Angular 21 com streaming SSE, state management via Signals e geração de minutas.
tags: [angular, typescript, signals, tailwind, sse, pnpm]
sources:
  - resource: "frontend/tjsc-ai/src/app/features/chat/shared/services/chat.service.ts"
    title: "chat.service.ts"
  - resource: "frontend/tjsc-ai/src/app/features/chat/shared/services/chat.api.service.ts"
    title: "chat.api.service.ts"
  - resource: "frontend/tjsc-ai/src/app/features/chat/shared/constants/api.constants.ts"
    title: "api.constants.ts"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# frontend-tjsc-ai

Interface de chat Angular 21. Porta **4200** (nginx em Docker, dev server local). Consome `process-agent:8083` via proxy.

## Stack

- Angular 21.2.0 — standalone components, sem NgModules
- TypeScript 5.9.2
- Signals nativos (sem NgRx, sem Zustand)
- Tailwind CSS 4.1.12 — engine nativa, sem PostCSS
- `marked 18.0.14` — renderização de Markdown
- pnpm 10.15.0

## State management — ChatService (Signals)

`ChatService` é a única fonte de verdade do estado da conversa:

```typescript
readonly conversationId = signal<string | null>(null);
readonly messages = signal<Message[]>([]);
readonly conversations = signal<ConversationSummary[]>([]);
readonly isStreaming = signal(false);
readonly isLoadingHistory = signal(false);

// Computed
readonly hasMessages = computed(() => this.messages().length > 0);
readonly conversationTitle = computed(() => ...);
```

`chat.store.ts` é apenas um re-export de `ChatService` — reservado para futura migração NgRx SignalStore.

## Streaming SSE — ChatApiService

Ver: [/patterns/sse-streaming.md](/patterns/sse-streaming.md)

`ChatApiService.streamMessage()` retorna `ReadableStream<string>`:
1. `fetch()` com `Accept: text/event-stream`
2. `response.body.pipeThrough(new TextDecoderStream()).getReader()`
3. Buffer acumulado, split por `'\n\n'` (separador SSE padrão)
4. Extrai linhas `data:`, filtra `[DONE]`
5. Detecta sentinela `[INSIGHT:{...}]` → chama `onInsightMeta(meta)` callback
6. Demais chunks → emite via `TransformStream`

`ChatService.sendMessage()` lê o stream em loop e acumula no último `messages()` via `ngZone.run()`.

## Mensagem interface

```typescript
interface Message {
  id: string;
  role: MessageRole;       // User | Assistant
  content: string;
  timestamp: Date;
  streaming?: boolean;     // true enquanto recebe chunks
  insightKey?: string;     // chave para refresh via cache
  fromCache?: boolean;     // veio do cache Redis
}
```

## Ciclo de vida de uma conversa

1. `startNewConversation()` → gera UUID com `crypto.randomUUID()`, limpa `messages`
2. `sendMessage(content)` → adiciona userMessage + assistantMessage(streaming: true) ao signal
3. Loop de chunks → atualiza `last.content += value` dentro de `ngZone.run()`
4. `finally` → `streaming: false`, `isStreaming.set(false)`, `loadConversations()`, `router.navigate`
5. `loadConversation(id)` → carrega histórico do MongoDB e popula `messages`

## refreshInsight

Botão de "atualizar" disponível em mensagens com `insightKey`:
```typescript
async refreshInsight(messageId: string): void {
  // Remove a mensagem do assistente + mensagem do usuário do sinal
  // Reenvia com bypassCache: true
}
```

## Rotas

```
/           → redireciona para /chat
/chat       → nova conversa (sem id)
/chat/:id   → conversa existente
```

## Componentes principais

| Componente | Responsabilidade |
|---|---|
| `chat-page` | Container principal, orquestra sidebar + lista + input |
| `conversation-sidebar` | Lista de conversas com renomear/favoritar/arquivar/deletar |
| `message-list` | Renderiza `messages()` signal |
| `message-item` | Renderiza uma mensagem com Markdown + indicador de cache |
| `chat-input` | Campo de texto + botão enviar/parar |
| `minuta-modal` | Interface para gerar e visualizar minutas |
| `processo-panel` | Painel lateral com movimentações/documentos do processo |
| `settings-modal` | Configurações: modelo, temperatura, system extra |

## Voice Live

Ver: [/services/voice-live.md](/services/voice-live.md)

`VoiceService` gerencia `RTCPeerConnection` + `RTCDataChannel` com uma máquina de 9 estados (Signals). Durante `voice.isActive`, o textarea do `chat-input` é substituído por um painel de ondas animadas com status e transcrição em tempo real. `ChatPageComponent` escuta `messageAdded` para recarregar o histórico a cada delegação concluída. `ChatService.setVoiceConversation(id)` seta o UUID sem disparo de `loadConversation`.

## Proxy para backend

`proxy.conf.json` redireciona `/api/v1/` → `http://localhost:8083/api/v1/` em desenvolvimento. Em Docker, nginx serve o frontend e faz proxy interno para `process-agent:8083`.

## API constants

```typescript
export const API = {
  conversations: '/api/v1/conversations',
  chatStream: '/api/v1/chat/stream',
  minutaGerar: '/api/v1/minuta/gerar',
  minutaHistorico: (numero) => `/api/v1/minuta/${encodeURIComponent(numero)}/historico`,
  minutaVersao: (numero, versao) => `/api/v1/minuta/${encodeURIComponent(numero)}/${versao}`,
}
```
