package br.jus.tjsc.ai.agent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=false",
        "spring.data.mongodb.auto-index-creation=false",
})
class ProcessAgentApplicationTests {

    @TestConfiguration
    static class TestOverrides {

        @Bean
        SyncMcpToolCallbackProvider mcpToolCallbacks() {
            SyncMcpToolCallbackProvider mock = Mockito.mock(SyncMcpToolCallbackProvider.class);
            Mockito.when(mock.getToolCallbacks()).thenReturn(new ToolCallback[0]);
            return mock;
        }

        @Bean
        @Primary
        RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }

        @Bean
        @Primary
        StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
            return Mockito.mock(StringRedisTemplate.class);
        }
    }

    @Test
    void contextLoads() {
    }
}
