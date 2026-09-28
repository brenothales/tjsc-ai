package br.jus.tjsc.ai.agent.minuta.infrastructure;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MinutaEventService {

    private final Map<String, Sinks.Many<String>> streams = new ConcurrentHashMap<>();

    public Flux<String> criar(String sessionId) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        streams.put(sessionId, sink);
        return sink.asFlux().doFinally(s -> streams.remove(sessionId));
    }

    public void publicar(String sessionId, String evento) {
        Optional.ofNullable(streams.get(sessionId))
                .ifPresent(s -> s.tryEmitNext(evento));
    }

    public void concluir(String sessionId) {
        Optional.ofNullable(streams.remove(sessionId))
                .ifPresent(Sinks.Many::tryEmitComplete);
    }

    public void erro(String sessionId, Throwable t) {
        Optional.ofNullable(streams.remove(sessionId))
                .ifPresent(s -> s.tryEmitError(t));
    }
}
