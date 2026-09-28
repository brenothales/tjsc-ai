package br.jus.tjsc.ai.agent.chat.application;

import br.jus.tjsc.ai.agent.chat.api.dto.ConversationDetail;
import br.jus.tjsc.ai.agent.chat.api.dto.ConversationPage;
import br.jus.tjsc.ai.agent.chat.api.dto.ConversationSummary;
import br.jus.tjsc.ai.agent.chat.domain.ConversationMetadata;
import br.jus.tjsc.ai.agent.chat.infrastructure.ConversationMetadataRepository;
import org.bson.Document;
import org.springframework.ai.chat.memory.repository.mongo.Conversation;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class ConversationServiceImpl implements ConversationService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final MongoChatMemoryRepository       chatMemoryRepository;
    private final ConversationMetadataRepository  metadataRepository;
    private final MongoTemplate                   mongoTemplate;
    private final String                          collectionName;

    ConversationServiceImpl(
            MongoChatMemoryRepository chatMemoryRepository,
            ConversationMetadataRepository metadataRepository,
            MongoTemplate mongoTemplate,
            @Value("${spring.ai.chat.memory.repository.mongodb.collection-name:ai_chat_memory}") String collectionName) {
        this.chatMemoryRepository = chatMemoryRepository;
        this.metadataRepository   = metadataRepository;
        this.mongoTemplate        = mongoTemplate;
        this.collectionName       = collectionName;
    }

    @Override
    public ConversationPage listConversations(int page, int size) {
        int effectiveSize = size > 0 ? size : DEFAULT_PAGE_SIZE;

        AggregationOperation groupStage = ctx -> new Document("$group", new Document("_id", "$conversationId")
                .append("createdAt",     new Document("$min", "$timestamp"))
                .append("lastMessageAt", new Document("$max", "$timestamp"))
                .append("messageCount",  new Document("$sum", 1))
                .append("userContents",  new Document("$push",
                        new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$message.type", "USER")),
                                "$message.content",
                                "$$REMOVE")))));

        AggregationOperation addTitleField = ctx -> new Document("$addFields",
                new Document("firstUserContent", new Document("$first", "$userContents")));

        AggregationOperation sortStage = ctx -> new Document("$sort", new Document("lastMessageAt", -1));

        AggregationResults<Document> countResult = mongoTemplate.aggregate(
                Aggregation.newAggregation(groupStage, ctx -> new Document("$count", "total")),
                collectionName, Document.class);
        long total = countResult.getMappedResults().isEmpty() ? 0L
                : ((Number) countResult.getMappedResults().get(0).get("total")).longValue();

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        groupStage, addTitleField, sortStage,
                        ctx -> new Document("$skip", (long) page * effectiveSize),
                        ctx -> new Document("$limit", effectiveSize)),
                collectionName, Document.class);

        List<String> pageIds = results.getMappedResults().stream()
                .map(d -> (String) d.get("_id")).toList();

        Map<String, ConversationMetadata> metadataMap = metadataRepository.findByConversationIdIn(pageIds)
                .stream().collect(Collectors.toMap(m -> m.conversationId, m -> m));

        List<ConversationSummary> content = results.getMappedResults().stream().map(d -> {
            String id          = (String) d.get("_id");
            ConversationMetadata meta = metadataMap.get(id);
            String customTitle = meta != null ? meta.customTitle : null;
            String title       = customTitle != null ? customTitle : truncate((String) d.get("firstUserContent"), 60);
            if (title == null || title.isBlank()) title = "Nova conversa";
            return new ConversationSummary(
                    id, title,
                    toInstant(d.get("createdAt")),
                    toInstant(d.get("lastMessageAt")),
                    ((Number) d.get("messageCount")).intValue(),
                    meta != null && meta.starred,
                    meta != null && meta.archived);
        }).toList();

        int totalPages = (int) Math.ceil((double) total / effectiveSize);
        return new ConversationPage(content, total, totalPages, page, effectiveSize);
    }

    @Override
    public ConversationDetail getConversation(String id) {
        List<Conversation> docs = fetchDocs(id);
        if (docs.isEmpty()) throw new ConversationNotFoundException(id);

        List<ConversationDetail.Message> messages = docs.stream()
                .filter(d -> "USER".equals(d.message().type()) || "ASSISTANT".equals(d.message().type()))
                .filter(d -> d.message().content() != null && !d.message().content().isBlank())
                .map(d -> new ConversationDetail.Message(d.message().type(), d.message().content(), d.timestamp()))
                .toList();

        if (messages.isEmpty()) throw new ConversationNotFoundException(id);
        return new ConversationDetail(id, messages);
    }

    @Override
    public void updateTitle(String id, String newTitle) {
        if (fetchDocs(id).isEmpty()) throw new ConversationNotFoundException(id);
        ConversationMetadata metadata = metadataRepository.findById(id)
                .orElse(new ConversationMetadata(id, null));
        metadata.customTitle = newTitle;
        metadata.updatedAt   = Instant.now();
        metadataRepository.save(metadata);
    }

    @Override
    public void setStarred(String id, boolean starred) {
        ConversationMetadata metadata = metadataRepository.findById(id)
                .orElse(new ConversationMetadata(id, null));
        metadata.starred   = starred;
        metadata.updatedAt = Instant.now();
        metadataRepository.save(metadata);
    }

    @Override
    public void setArchived(String id, boolean archived) {
        ConversationMetadata metadata = metadataRepository.findById(id)
                .orElse(new ConversationMetadata(id, null));
        metadata.archived  = archived;
        metadata.updatedAt = Instant.now();
        metadataRepository.save(metadata);
    }

    @Override
    public void deleteConversation(String id) {
        chatMemoryRepository.deleteByConversationId(id);
        metadataRepository.deleteById(id);
    }

    private List<Conversation> fetchDocs(String id) {
        return mongoTemplate.find(
                Query.query(Criteria.where("conversationId").is(id))
                        .with(Sort.by(Sort.Direction.ASC, "seq", "timestamp")),
                Conversation.class, collectionName);
    }

    private static Instant toInstant(Object value) {
        if (value instanceof java.util.Date d) return d.toInstant();
        if (value instanceof Instant i) return i;
        return null;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }
}
