package br.jus.tjsc.ai.agent.chat.application;

import br.jus.tjsc.ai.agent.chat.api.dto.ChatRequest;
import br.jus.tjsc.ai.agent.chat.api.dto.ChatResponse;
import br.jus.tjsc.ai.agent.chat.infrastructure.InsightCacheService;
import br.jus.tjsc.ai.agent.chat.infrastructure.ProcessStatusChecker;
import br.jus.tjsc.ai.agent.chat.infrastructure.ai.AgentService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.UUID;

@Service
class ChatServiceImpl implements ChatService {

    static final String INSIGHT_SENTINEL_PREFIX = "[INSIGHT:";
    static final String INSIGHT_SENTINEL_SUFFIX = "]";

    private final AgentService         agentService;
    private final InsightCacheService  insightCache;
    private final ProcessStatusChecker statusChecker;

    ChatServiceImpl(AgentService agentService,
                    InsightCacheService insightCache,
                    ProcessStatusChecker statusChecker) {
        this.agentService  = agentService;
        this.insightCache  = insightCache;
        this.statusChecker = statusChecker;
    }

    @Override
    public Flux<String> stream(ChatRequest request) {
        String conversationId = resolveConversationId(request);

        if (request.insightKey() != null && !request.insightKey().isBlank()) {
            return streamWithCache(request, conversationId);
        }

        return agentService.chat(request.message(), conversationId,
                request.model(), request.temperature(), request.systemExtra());
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request);
        String content = agentService.chatSync(request.message(), conversationId,
                request.model(), request.temperature(), request.systemExtra());
        return new ChatResponse(conversationId, content);
    }

    private Flux<String> streamWithCache(ChatRequest request, String conversationId) {
        String key    = request.insightKey();
        boolean bypass = Boolean.TRUE.equals(request.bypassCache());

        if (!bypass) {
            Optional<String> cached = insightCache.get(key);
            if (cached.isPresent()) {
                String meta = sentinelMeta(key, true);
                return Flux.just(meta, cached.get(), "[DONE]");
            }
        }

        if (bypass) insightCache.evict(key);

        String numero = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        StringBuilder buffer = new StringBuilder();

        return Flux.concat(
                Flux.just(sentinelMeta(key, false)),
                agentService.chat(request.message(), conversationId,
                                request.model(), request.temperature(), request.systemExtra())
                        .doOnNext(buffer::append)
                        .doOnComplete(() -> {
                            if (statusChecker.canCache(numero)) {
                                insightCache.set(key, buffer.toString());
                            }
                        })
        );
    }

    private static String sentinelMeta(String key, boolean fromCache) {
        String safeKey = key.replace("\\", "\\\\").replace("\"", "\\\"");
        return INSIGHT_SENTINEL_PREFIX
                + "{\"fromCache\":" + fromCache + ",\"insightKey\":\"" + safeKey + "\"}"
                + INSIGHT_SENTINEL_SUFFIX;
    }

    private static String resolveConversationId(ChatRequest request) {
        return (request.conversationId() != null && !request.conversationId().isBlank())
                ? request.conversationId()
                : UUID.randomUUID().toString();
    }
}
