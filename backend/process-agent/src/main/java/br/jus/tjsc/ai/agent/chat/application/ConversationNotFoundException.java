package br.jus.tjsc.ai.agent.chat.application;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(String id) {
        super("Conversa não encontrada: " + id);
    }
}
