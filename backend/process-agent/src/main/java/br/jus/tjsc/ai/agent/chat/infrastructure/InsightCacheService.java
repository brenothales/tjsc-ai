package br.jus.tjsc.ai.agent.chat.infrastructure;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class InsightCacheService {

    private static final String PREFIX = "insight:";
    private static final Duration TTL  = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    InsightCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<String> get(String insightKey) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + insightKey));
    }

    public void set(String insightKey, String content) {
        redis.opsForValue().set(PREFIX + insightKey, content, TTL);
    }

    public void evict(String insightKey) {
        redis.delete(PREFIX + insightKey);
    }
}
