package br.jus.tjsc.ai.agent.voice.api;

import br.jus.tjsc.ai.agent.chat.infrastructure.ai.AgentService;
import br.jus.tjsc.ai.agent.voice.api.dto.VoiceAgentRequest;
import br.jus.tjsc.ai.agent.voice.api.dto.VoiceAgentResponse;
import br.jus.tjsc.ai.agent.voice.api.dto.VoiceSessionRequest;
import br.jus.tjsc.ai.agent.voice.api.dto.VoiceSessionResponse;
import br.jus.tjsc.ai.agent.voice.application.VoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/voice")
class VoiceController {

    private final VoiceService    voiceService;
    private final AgentService    agentService;

    VoiceController(VoiceService voiceService, AgentService agentService) {
        this.voiceService = voiceService;
        this.agentService = agentService;
    }

    @PostMapping("/session")
    VoiceSessionResponse createSession(@RequestBody VoiceSessionRequest request) throws com.fasterxml.jackson.core.JsonProcessingException {
        return voiceService.createSession(request.sdp());
    }

    @PostMapping("/agent")
    VoiceAgentResponse query(@RequestBody VoiceAgentRequest request) {
        String conversationId = (request.conversationId() != null && !request.conversationId().isBlank())
                ? request.conversationId()
                : UUID.randomUUID().toString();
        String content = agentService.chatSync(request.message(), conversationId, null, null, null);
        return new VoiceAgentResponse(content);
    }
}
