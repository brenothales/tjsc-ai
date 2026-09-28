package br.jus.tjsc.ai.agent.minuta.infrastructure;

import br.jus.tjsc.ai.agent.minuta.domain.MinutaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MinutaRepository extends MongoRepository<MinutaDocument, String> {
    List<MinutaDocument> findByNumeroOrderByVersaoMinutaDesc(String numero);
    Optional<MinutaDocument> findByNumeroAndVersaoMinuta(String numero, int versaoMinuta);
}
