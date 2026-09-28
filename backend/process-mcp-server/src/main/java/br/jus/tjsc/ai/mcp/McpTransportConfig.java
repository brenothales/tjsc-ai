package br.jus.tjsc.ai.mcp;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class McpTransportConfig {

    @Bean
    HttpServletStreamableServerTransportProvider streamableTransportProvider() {
        return new HttpServletStreamableServerTransportProvider.Builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServletRegistration(
            HttpServletStreamableServerTransportProvider transport) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> reg =
                new ServletRegistrationBean<>(transport, "/mcp/*");
        reg.setName("mcpStreamableServlet");
        return reg;
    }
}
