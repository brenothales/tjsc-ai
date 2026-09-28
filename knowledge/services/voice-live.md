---
type: Service
title: voice-live
description: Conversa por voz em tempo real via WebRTC + OpenAI Realtime API, integrada ao process-agent e ao chat Angular.
tags: [angular, webrtc, openai-realtime, spring-boot, voice, typescript]
sources:
  - resource: "frontend/tjsc-ai/src/app/features/chat/shared/services/voice.service.ts"
    title: "voice.service.ts"
  - resource: "frontend/tjsc-ai/src/app/features/chat/components/voice-button/voice-button.ts"
    title: "voice-button.ts"
  - resource: "frontend/tjsc-ai/src/app/features/chat/components/chat-input/chat-input.html"
    title: "chat-input.html"
  - resource: "frontend/tjsc-ai/src/app/features/chat/components/chat-input/chat-input.css"
    title: "chat-input.css"
  - resource: "frontend/tjsc-ai/src/app/features/chat/components/chat-page/chat-page.ts"
    title: "chat-page.ts"
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/voice/application/McpToolRegistry.java"
    title: "McpToolRegistry.java"
  - resource: "backend/process-agent/src/main/resources/application.yml"
    title: "application.yml (voice.live)"
  - resource: "backend/process-agent/src/main/resources/prompts/voice_system.st"
    title: "voice_system.st"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-28T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: in-progress
---

# voice-live

Funcionalidade de conversa por voz em tempo real. O usuário fala diretamente com um modelo GPT-Live via WebRTC; perguntas que exigem consulta processual são delegadas ao `process-agent` (que usa MCP tools e MongoDB). Tudo acontece dentro da conversa de texto existente — sem nova rota, sem perda de histórico.

## Arquitetura

```
Usuário (microfone)
    │  WebRTC (audio track)
    ▼
RTCPeerConnection  ◄──── SDP negociado via POST /api/v1/voice/session
    │  RTCDataChannel "oai-events"  (JSON bidirectional)
    ▼
OpenAI Realtime API (gpt-live-1)
    │  session.delegation.created  (tool call do LLM de voz)
    ▼
VoiceService.handleDelegation()
    │  POST /api/v1/chat (síncrono)
    ▼
process-agent  →  MCP tools  →  process-data-service
    │  { content: "..." }
    ▼
session.commentary.append  →  GPT-Live fala a resposta ao usuário
```

## Configuração backend (application.yml)

```yaml
voice:
  live:
    sessions-path: /v1/live/sessions   # path relativo à OpenAI base-url
    model: ${OPENAI_LIVE_MODEL:gpt-live-1}
    voice: ${OPENAI_LIVE_VOICE:verse}
    system-prompt: classpath:prompts/voice_system.st
```

Nenhum valor fixo no código Java — tudo via YAML com variáveis de ambiente opcionais.

## McpToolRegistry (backend)

`McpToolRegistry` converte as 14 ferramentas MCP em formato `function` do OpenAI Live para injeção na sessão:

```java
Map.of(
    "type",        "function",
    "name",        definition.name(),
    "description", definition.description(),
    "parameters",  parseSchema(definition.inputSchema())   // JsonNode via ObjectMapper
)
```

Usa `ToolCallbackProvider` injetado pelo Spring AI (mesmo bean das ferramentas MCP do chat).

## SDP — por que RestClient

A criação da sessão exige negociar o SDP (oferta/resposta WebRTC) em uma única chamada HTTP bloqueante. `RestClient` é suficiente para esse caso; `ChatClient` cuida das consultas delegadas (`/api/v1/chat`).

## VoiceService (frontend)

Máquina de estados gerenciada com Signals:

```
idle → connecting → listening → user_speaking → processing → agent_speaking
                                                           → tool_executing
                             ← (response.done)
any → closing → idle         (session.close / session.closed)
any → error                  (RTCPeerConnection failed, fetch error)
```

### Estados e seus significados

| Estado | Gatilho | Label exibido |
|---|---|---|
| `idle` | inicial / após parar | — |
| `connecting` | `start()` chamado | "Conectando ao assistente…" |
| `listening` | `session.started` | "Estou ouvindo. Pode continuar falando." |
| `user_speaking` | `input_audio_buffer.speech_started` | "Ouvindo você" |
| `processing` | `input_audio_buffer.speech_stopped` | "Preparando uma resposta…" |
| `agent_speaking` | `response.audio.delta` | "O assistente está respondendo" |
| `tool_executing` | `session.delegation.created` | "Consultando informações…" |
| `closing` | `stop()` com DC aberto | "Encerrando a sessão de voz…" |
| `error` | falha de conexão ou fetch | mensagem de erro |

### Sinais públicos

```typescript
readonly state               = signal<VoiceState>('idle');
readonly transcript          = signal<string>('');      // fala do usuário em curso
readonly agentText           = signal<string>('');      // transcrição do agente em curso
readonly errorMessage        = signal<string>('');
readonly voiceConversationId = signal<string | null>(null);
readonly messageAdded        = signal(0);               // incrementado a cada delegação concluída
```

### Encerramento gracioso

`stop()` envia `session.close` via DataChannel e aguarda `session.closed` (timeout 15 s). Só chama `cleanup()` após confirmação — evita deixar sessão aberta no servidor.

### Delegação (consulta processual)

Ao receber `session.delegation.created`:
1. Captura `transcript()` como query
2. POST `/api/v1/chat` com `{ message: query, conversationId }`
3. Injeta resposta no LLM de voz via `session.commentary.append`
4. Incrementa `messageAdded` para disparar recarga do histórico

### Interrupção

Se usuário começa a falar enquanto agente responde (`agent_speaking`), envia `response.cancel` automaticamente.

## Integração no frontend

### ChatInputComponent

Durante `voice.isActive`, o textarea e o botão enviar são substituídos por um painel de ondas animadas:

```html
@if (voice.isActive) {
  <div class="voice-live-panel" [class.voice-live-panel--speaking]="...">
    <div class="voice-wave">  <!-- 17 barras animadas -->
    <p>{{ voice.statusLabel() }}</p>
    <p>{{ voice.caption() }}</p>  <!-- transcript ou agentText -->
  </div>
}
```

Classes CSS de estado:
- `voice-live-panel--speaking` (user_speaking | agent_speaking) → barras verdes animadas
- `voice-live-panel--working` (processing | tool_executing | closing) → barras âmbar, paradas

### ChatPageComponent — efeito de recarga

```typescript
effect(() => {
  const n = this.voice.messageAdded();
  if (n === 0) return;
  const voiceId = this.voice.voiceConversationId();
  if (voiceId && voiceId === this.chat.conversationId()) {
    this.chat.loadConversation(voiceId);
    this.chat.loadConversations();
  }
});
```

Recarrega histórico toda vez que uma delegação conclui, mantendo a lista de mensagens atualizada sem navegar.

### ChatService — setVoiceConversation

```typescript
setVoiceConversation(id: string): void {
  this.conversationId.set(id);
  this.messages.set([]);
}
```

Usado quando a rota muda para o ID da conversa de voz em andamento: seta o ID sem disparar `loadConversation` (que sobrescreveria mensagens mid-stream).

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `OPENAI_LIVE_MODEL` | `gpt-live-1` | Modelo GPT-Live |
| `OPENAI_LIVE_VOICE` | `verse` | Voz do modelo |
