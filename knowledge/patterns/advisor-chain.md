---
type: Architecture Pattern
title: Advisor Chain — ordem determinística dos advisors
description: Os 5 advisors do ChatClient são compostos em ordem de precedência específica que garante segurança, idioma, memória e logging na sequência correta.
tags: [spring-ai, advisor, guardrail, chat-memory]
sources:
  - resource: "backend/process-agent/src/main/java/br/jus/tjsc/ai/agent/chat/infrastructure/ai/AgentService.java"
    title: "AgentService.java"
generated:
  by: "TJSC AI — Diretoria de Tecnologia da Informação"
  at: "2026-09-26T00:00:00Z"
verified:
  by: "Líder de Produtos de IA · Breno Thales"
  at: "2026-09-28T00:00:00Z"
status: stable
---

# Advisor Chain — Ordem Determinística

O `ChatClient` do `AgentService` é configurado com 5 advisors. A ordem não é arbitrária — cada advisor precisa enxergar o estado correto da requisição.

## Ordem de execução

```
Requisição do usuário
    │
    ▼  HIGHEST_PRECEDENCE (outermost)
InputGuardrailAdvisor
    │  Bloqueia: prompt injection, mensagem muito longa
    │  Se violation → short-circuit (não chama próximo)
    ▼
LanguageEnforcementAdvisor  (HIGHEST_PRECEDENCE + 1)
    │  Injeta instrução no system prompt:
    │  "RESPONDA SEMPRE EM PORTUGUÊS DO BRASIL"
    ▼
MessageChatMemoryAdvisor  (MongoDB)
    │  Recupera até 20 mensagens anteriores do conversationId
    │  Injeta histórico no contexto
    ▼
SimpleLoggerAdvisor
    │  Loga request/response (nível DEBUG)
    ▼
LLM (GPT-4o-mini) + MCP Tools (GuardedToolCallback)
    │
    ▼
OutputGuardrailAdvisor  (LOWEST_PRECEDENCE — innermost)
    │  Sanitiza resposta: detecta API keys, senhas, URIs MongoDB
    ▼
Resposta entregue ao usuário
```

## Por que essa ordem?

- **InputGuardrail primeiro**: rejeita mensagens maliciosas sem custo de tokens nem acesso à memória. Um atacante não deve chegar ao LLM.
- **LanguageEnforcement segundo**: força idioma antes de carregar a memória — mensagens multilíngues não "contamina" o contexto com instruções no idioma errado.
- **ChatMemory terceiro**: carrega histórico apenas após as verificações de segurança. Evita que histórico com conteúdo malicioso seja consultado desnecessariamente.
- **Logger quarto**: registra o estado final da requisição (com histórico já injetado) para observabilidade real.
- **OutputGuardrail por último**: vê a resposta completa do LLM. É o "último filtro" antes da entrega.

## Implementação

```java
// AgentService.java — construtor
this.chatClient = builder
    .defaultTools(mcpTools)
    .defaultAdvisors(
        inputGuardrailAdvisor,           // HIGHEST_PRECEDENCE
        languageEnforcementAdvisor,      // HIGHEST_PRECEDENCE + 1
        MessageChatMemoryAdvisor.builder(chatMemory).build(),
        new SimpleLoggerAdvisor(),
        outputGuardrailAdvisor)          // LOWEST_PRECEDENCE
    .build();
```

## systemExtra

O `AgentService.buildPrompt()` aceita `systemExtra` — string concatenada ao `agent_system.st`:
```java
String systemText = (systemExtra != null && !systemExtra.isBlank())
    ? baseSystemPrompt + "\n\n## Instruções adicionais do usuário\n" + systemExtra
    : baseSystemPrompt;
```
Permite que o frontend (settings-modal) injete instruções adicionais sem modificar o system prompt base.
