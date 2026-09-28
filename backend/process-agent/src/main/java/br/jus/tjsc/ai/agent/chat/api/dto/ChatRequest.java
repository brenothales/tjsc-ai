package br.jus.tjsc.ai.agent.chat.api.dto;

public record ChatRequest(
        String conversationId,
        String message,
        String insightKey,
        Boolean bypassCache,
        String model,
        Double temperature,
        String systemExtra
) {}
