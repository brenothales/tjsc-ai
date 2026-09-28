package br.jus.tjsc.ai.agent.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    MongoClient mongoClient(
            @Value("${spring.data.mongodb.host:localhost}") String host,
            @Value("${spring.data.mongodb.port:27017}") int port,
            @Value("${spring.data.mongodb.username:tjsc}") String username,
            @Value("${spring.data.mongodb.password:tjsc}") String password,
            @Value("${spring.data.mongodb.authentication-database:admin}") String authDb) {

        MongoCredential credential = MongoCredential.createCredential(username, authDb, password.toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .applyToClusterSettings(b -> b.hosts(java.util.List.of(new ServerAddress(host, port))))
                .build();
        log.info("Creating MongoClient for {}:{} with user={} authDb={}", host, port, username, authDb);
        return MongoClients.create(settings);
    }

    @Bean
    MongoTemplate mongoTemplate(MongoClient mongoClient,
            @Value("${spring.data.mongodb.database:process-agent}") String database) {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, database));
    }

    @Bean
    MongoChatMemoryRepository chatMemoryRepository(MongoTemplate mongoTemplate) {
        return MongoChatMemoryRepository.builder()
                .mongoTemplate(mongoTemplate)
                .build();
    }

    @Bean
    @Primary
    ChatMemory chatMemory(MongoChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}
