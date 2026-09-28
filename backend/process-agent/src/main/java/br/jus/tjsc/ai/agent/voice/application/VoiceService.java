package br.jus.tjsc.ai.agent.voice.application;

import br.jus.tjsc.ai.agent.voice.api.dto.VoiceSessionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class VoiceService {

    private static final Logger log = LoggerFactory.getLogger(VoiceService.class);

    private static final String OPENAI_BASE_URL = "https://api.openai.com";
    private static final String LIVE_SESSIONS   = "/v1/live/sessions";
    private static final String MODEL           = "gpt-live-1";
    private static final String VOICE           = "verse";

    private final RestClient   restClient;
    private final String       systemPrompt;
    private final ObjectMapper objectMapper = new ObjectMapper();

    VoiceService(@Value("${spring.ai.openai.api-key}") String apiKey,
                 @Value("classpath:prompts/agent_system.st") Resource systemPromptResource) throws IOException {
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public VoiceSessionResponse createSession(String sdpOffer) throws com.fasterxml.jackson.core.JsonProcessingException {
        log.info("[VoiceService] criando sessão Live — model={} voice={}", MODEL, VOICE);

        Map<String, Object> sessionConfig = Map.of(
                "model", MODEL,
                "instructions", systemPrompt,
                "delegation", Map.of("type", "client")
        );

        Map<String, Object> body = Map.of(
                "session", sessionConfig,
                "transport", Map.of(
                        "type", "webrtc",
                        "sdp", sdpOffer
                )
        );

        String raw = restClient.post()
                .uri(LIVE_SESSIONS)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode response = objectMapper.readTree(raw);
        String sessionId = response.path("session").path("id").asText();
        String sdpAnswer = response.path("transport").path("sdp").asText();

        log.info("[VoiceService] sessão criada — id={}", sessionId);
        return new VoiceSessionResponse(sessionId, sdpAnswer);
    }
}
