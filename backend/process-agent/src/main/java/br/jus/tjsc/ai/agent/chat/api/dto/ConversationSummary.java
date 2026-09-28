package br.jus.tjsc.ai.agent.chat.api.dto;

import java.time.Instant;

public record ConversationSummary(
        String id,
        String title,
        Instant createdAt,
        Instant lastMessageAt,
        int messageCount,
        boolean starred,
        boolean archived
) {}
