package br.jus.tjsc.ai.agent.voice.api;

import br.jus.tjsc.ai.agent.chat.infrastructure.ai.AgentService;
import br.jus.tjsc.ai.agent.voice.api.dto.VoiceAgentRequest;
import br.jus.tjsc.ai.agent.voice.api.dto.VoiceAgentResponse;
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
    VoiceSessionResponse createSession() {
        return voiceService.createSession();
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
