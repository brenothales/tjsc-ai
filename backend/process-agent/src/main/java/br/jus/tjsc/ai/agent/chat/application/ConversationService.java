package br.jus.tjsc.ai.agent.chat.application;

import br.jus.tjsc.ai.agent.chat.api.dto.ConversationDetail;
import br.jus.tjsc.ai.agent.chat.api.dto.ConversationPage;

public interface ConversationService {
    ConversationPage listConversations(int page, int size);
    ConversationDetail getConversation(String id);
    void updateTitle(String id, String newTitle);
    void setStarred(String id, boolean starred);
    void setArchived(String id, boolean archived);
    void deleteConversation(String id);
}
