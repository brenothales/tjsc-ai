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
import java.util.List;
import java.util.Map;

@Service
public class VoiceService {

    private static final Logger log = LoggerFactory.getLogger(VoiceService.class);

    private static final String OPENAI_BASE_URL   = "https://api.openai.com";
    private static final String REALTIME_SESSIONS = "/v1/realtime/sessions";
    private static final String MODEL             = "gpt-4o-realtime-preview-2024-12-17";
    private static final String VOICE             = "verse";

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

    public VoiceSessionResponse createSession() {
        log.info("[VoiceService] criando sessão Realtime — model={} voice={}", MODEL, VOICE);

        Map<String, Object> consultarAgenteTool = Map.of(
                "type", "function",
                "name", "consultar_agente",
                "description", "Consulta o agente institucional do TJSC para responder perguntas sobre processos, partes, magistrados e movimentações.",
                "parameters", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pergunta", Map.of(
                                        "type", "string",
                                        "description", "A pergunta ou consulta a ser respondida pelo agente"
                                )
                        ),
                        "required", List.of("pergunta")
                )
        );

        Map<String, Object> body = Map.of(
                "model", MODEL,
                "voice", VOICE,
                "instructions", systemPrompt,
                "input_audio_transcription", Map.of("model", "whisper-1"),
                "turn_detection", Map.of("type", "server_vad"),
                "tools", List.of(consultarAgenteTool),
                "tool_choice", "auto"
        );

        JsonNode response = restClient.post()
                .uri(REALTIME_SESSIONS)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String sessionId    = response.path("id").asText();
        String clientSecret = response.path("client_secret").path("value").asText();
        long   expiresAt    = response.path("client_secret").path("expires_at").asLong();

        log.info("[VoiceService] sessão criada — id={} expiresAt={}", sessionId, expiresAt);
        return new VoiceSessionResponse(sessionId, clientSecret, expiresAt);
    }
}
