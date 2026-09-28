package br.jus.tjsc.ai.agent.voice.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "voice.live")
public class VoiceLiveProperties {

    private String sessionsPath = "/v1/live/sessions";
    private String model = "gpt-live-1";
    private String voice = "verse";

    public String getSessionsPath() {
        return sessionsPath;
    }

    public void setSessionsPath(String sessionsPath) {
        this.sessionsPath = sessionsPath;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }
}
