package br.jus.tjsc.ai.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
class ProcessDataConfig {

    @Bean
    ProcessDataApi processDataApi(@Value("${process.data.service.url}") String baseUrl) {
        return httpServiceProxyFactory(baseUrl).createClient(ProcessDataApi.class);
    }

    @Bean
    SqlDataApi sqlDataApi(@Value("${process.data.service.url}") String baseUrl) {
        return httpServiceProxyFactory(baseUrl).createClient(SqlDataApi.class);
    }

    private HttpServiceProxyFactory httpServiceProxyFactory(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Accept", "application/json")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
    }
}
