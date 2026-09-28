package br.jus.tjsc.ai.agent.minuta.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("minutas")
public class MinutaDocument {

    @Id private String id;
    @Indexed private String numero;
    private int versaoMinuta;
    private Instant criadaEm;
    private List<MinutaVersao> versoes;

    public MinutaDocument() {}

    public MinutaDocument(String numero, int versaoMinuta, List<MinutaVersao> versoes) {
        this.numero       = numero;
        this.versaoMinuta = versaoMinuta;
        this.criadaEm     = Instant.now();
        this.versoes      = versoes;
    }

    public String getId()               { return id; }
    public String getNumero()           { return numero; }
    public int getVersaoMinuta()        { return versaoMinuta; }
    public Instant getCriadaEm()        { return criadaEm; }
    public List<MinutaVersao> getVersoes() { return versoes; }
}
