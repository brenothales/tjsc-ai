package br.jus.tjsc.ai.agent.chat.api.dto;

import java.time.Instant;
import java.util.List;

public record ConversationDetail(String id, List<ConversationDetail.Message> messages) {
    public record Message(String role, String content, Instant timestamp) {}
}
