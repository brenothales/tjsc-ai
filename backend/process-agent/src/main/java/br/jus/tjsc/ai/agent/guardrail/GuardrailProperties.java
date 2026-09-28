package br.jus.tjsc.ai.agent.guardrail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "guardrail")
public class GuardrailProperties {

    private Input input = new Input();
    private Output output = new Output();

    public Input getInput() { return input; }
    public void setInput(Input input) { this.input = input; }

    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }

    public static class Input {

        private int maxMessageLength = 2000;

        private List<String> injectionPatterns = List.of(
                "ignore\\s+(todas\\s+as\\s+)?instru[çc][oõ]es",
                "ignore\\s+(as\\s+)?regras(\\s+de\\s+seguran[çc]a)?",
                "mostre?\\s+(o\\s+)?system\\s+prompt",
                "mostre?\\s+(suas?\\s+)?instru[çc][oõ]es\\s+internas",
                "aja\\s+como\\s+se\\s+voc[êe]\\s+fosse",
                "pretenda\\s+ser",
                "\\bDAN\\b",
                "ignore\\s+tudo\\s+acima",
                "esqueça\\s+(tudo|todas|seus|suas)",
                "novo\\s+papel",
                "sem\\s+restri[çc][oõ]es",
                "voc[êe]\\s+n[ãa]o\\s+tem\\s+restri[çc][oõ]es",
                "revele\\s+(o\\s+)?(seu\\s+)?prompt"
        );

        private String injectionResponse =
                "Sua mensagem foi identificada como potencialmente maliciosa e não pôde ser processada.";

        private String sizeExceededResponse =
                "Sua mensagem é muito longa. Por favor, reformule de forma mais concisa.";

        public int getMaxMessageLength() { return maxMessageLength; }
        public void setMaxMessageLength(int maxMessageLength) { this.maxMessageLength = maxMessageLength; }

        public List<String> getInjectionPatterns() { return injectionPatterns; }
        public void setInjectionPatterns(List<String> injectionPatterns) { this.injectionPatterns = injectionPatterns; }

        public String getInjectionResponse() { return injectionResponse; }
        public void setInjectionResponse(String injectionResponse) { this.injectionResponse = injectionResponse; }

        public String getSizeExceededResponse() { return sizeExceededResponse; }
        public void setSizeExceededResponse(String sizeExceededResponse) { this.sizeExceededResponse = sizeExceededResponse; }
    }

    public static class Output {

        private List<String> sensitivePatterns = List.of(
                "sk-proj-[A-Za-z0-9_-]{20,}",
                "sk-[A-Za-z0-9]{48}",
                "OPENAI_API_KEY",
                "mongodb://[^\\s]*:[^\\s]*@",
                "(?i)password\\s*[:=]\\s*\\S+",
                "(?i)secret\\s*[:=]\\s*\\S+"
        );

        private String sanitizedResponse =
                "A resposta foi filtrada por conter informações potencialmente sensíveis.";

        public List<String> getSensitivePatterns() { return sensitivePatterns; }
        public void setSensitivePatterns(List<String> sensitivePatterns) { this.sensitivePatterns = sensitivePatterns; }

        public String getSanitizedResponse() { return sanitizedResponse; }
        public void setSanitizedResponse(String sanitizedResponse) { this.sanitizedResponse = sanitizedResponse; }
    }
}
