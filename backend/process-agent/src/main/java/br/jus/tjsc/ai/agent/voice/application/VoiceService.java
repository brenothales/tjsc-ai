package br.jus.tjsc.ai.agent.voice.application;

import br.jus.tjsc.ai.agent.voice.api.dto.VoiceSessionResponse;
import com.fasterxml.jackson.databind.JsonNode;
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

    private static final String OPENAI_BASE_URL    = "https://api.openai.com";
    private static final String REALTIME_SESSIONS  = "/v1/realtime/sessions";
    private static final String MODEL = "gpt-4o-realtime-preview-2024-12-17";
    private static final String VOICE = "verse";

    private final RestClient restClient;
    private final String systemPrompt;

    VoiceService(@Value("${spring.ai.openai.api-key}") String apiKey,
                 @Value("classpath:prompts/agent_system.st") Resource systemPromptResource) throws IOException {
        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("OpenAI-Beta", "realtime=v1")
                .build();
        this.systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public VoiceSessionResponse createSession() {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "voice", VOICE,
                "instructions", systemPrompt,
                "input_audio_transcription", Map.of("model", "whisper-1"),
                "turn_detection", Map.of("type", "server_vad")
        );

        log.info("[VoiceService] POST {}{} model={}", OPENAI_BASE_URL, REALTIME_SESSIONS, MODEL);

        JsonNode response = restClient.post()
                .uri(REALTIME_SESSIONS)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        return new VoiceSessionResponse(
                response.path("id").asText(),
                response.path("client_secret").path("value").asText(),
                response.path("client_secret").path("expires_at").asLong()
        );
    }
}
