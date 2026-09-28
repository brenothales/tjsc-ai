package br.jus.tjsc.ai.agent.chat.api.dto;

import java.util.List;

public record ConversationPage(
        List<ConversationSummary> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {}
