package br.jus.tjsc.ai.agent.voice.application;

import br.jus.tjsc.ai.agent.voice.api.dto.VoiceSessionResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

    private final RestClient   restClient;
    private final String       systemPrompt;
    private final VoiceLiveProperties voiceProperties;
    private final ObjectMapper objectMapper;

    VoiceService(RestClient.Builder restClientBuilder,
                 VoiceLiveProperties voiceProperties,
                 ObjectMapper objectMapper,
                 @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
                 @Value("${spring.ai.openai.api-key}") String apiKey,
                 @Value("${voice.live.system-prompt:classpath:prompts/voice_system.st}") Resource systemPromptResource) throws IOException {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.voiceProperties = voiceProperties;
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public VoiceSessionResponse createSession(String sdpOffer) throws tools.jackson.core.JacksonException {
        log.info("[VoiceService] criando sessão Live — model={} voice={}",
                voiceProperties.getModel(), voiceProperties.getVoice());

        Map<String, Object> sessionConfig = Map.of(
                "model", voiceProperties.getModel(),
                "instructions", systemPrompt,
                "delegation", Map.of("type", "client"),
                "audio", Map.of("output", Map.of("voice", voiceProperties.getVoice()))
        );

        Map<String, Object> body = Map.of(
                "session", sessionConfig,
                "transport", Map.of(
                        "type", "webrtc",
                        "sdp", sdpOffer
                )
        );

        String raw = restClient.post()
                .uri(voiceProperties.getSessionsPath())
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
