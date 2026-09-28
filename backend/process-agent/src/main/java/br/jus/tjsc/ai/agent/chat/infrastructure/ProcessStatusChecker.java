package br.jus.tjsc.ai.agent.chat.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ProcessStatusChecker {

    private static final Logger log = LoggerFactory.getLogger(ProcessStatusChecker.class);

    private final RestClient restClient;

    ProcessStatusChecker(@Value("${process.data-service.url:http://localhost:8081}") String dataServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(dataServiceUrl).build();
    }

    public boolean canCache(String numeroProcesso) {
        try {
            String response = restClient.get()
                    .uri("/api/v1/processos/{numero}", numeroProcesso)
                    .retrieve()
                    .body(String.class);
            if (response == null) return false;
            return !response.toLowerCase().contains("andamento");
        } catch (Exception e) {
            log.warn("Não foi possível verificar situação do processo {} para cache: {}", numeroProcesso, e.getMessage());
            return false;
        }
    }
}
