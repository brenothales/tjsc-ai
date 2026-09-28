package br.jus.tjsc.ai.process.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Process Data Service — TJSC")
                        .description("API REST somente leitura para consulta de dados processuais do Tribunal de Justiça de Santa Catarina. " +
                                "Sem LLM, sem MCP, sem SQL arbitrário.")
                        .version("0.0.1-SNAPSHOT"));
    }
}
