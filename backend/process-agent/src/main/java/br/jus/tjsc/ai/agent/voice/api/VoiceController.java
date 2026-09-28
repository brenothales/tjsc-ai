package br.jus.tjsc.ai.agent.voice.api;

import br.jus.tjsc.ai.agent.voice.api.dto.VoiceSessionResponse;
import br.jus.tjsc.ai.agent.voice.application.VoiceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/voice")
class VoiceController {

    private final VoiceService voiceService;

    VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/session")
    VoiceSessionResponse createSession() {
        return voiceService.createSession();
    }
}
