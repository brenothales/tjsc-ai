package br.jus.tjsc.ai.agent.chat.infrastructure;

import br.jus.tjsc.ai.agent.chat.domain.ConversationMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConversationMetadataRepository extends MongoRepository<ConversationMetadata, String> {
    List<ConversationMetadata> findByConversationIdIn(List<String> ids);
}
