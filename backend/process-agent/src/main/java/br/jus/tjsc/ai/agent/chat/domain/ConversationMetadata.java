package br.jus.tjsc.ai.agent.chat.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("conversation_metadata")
public class ConversationMetadata {

    @Id
    public String conversationId;
    public String customTitle;
    public Instant updatedAt;
    public boolean starred;
    public boolean archived;

    public ConversationMetadata() {}

    public ConversationMetadata(String conversationId, String customTitle) {
        this.conversationId = conversationId;
        this.customTitle    = customTitle;
        this.updatedAt      = Instant.now();
    }
}
