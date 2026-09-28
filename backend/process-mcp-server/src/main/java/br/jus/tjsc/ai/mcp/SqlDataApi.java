package br.jus.tjsc.ai.mcp;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

@HttpExchange("/api/v1/sql")
interface SqlDataApi {

    @PostExchange
    String executarSql(@RequestBody Map<String, String> body);
}
