package br.jus.tjsc.ai.process.adapter.out.sqlite;

import br.jus.tjsc.ai.process.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SQLiteProcessoRepositoryTest {

    private static final String NUMERO = "0000001-02.2019.8.99.9018";
    private static final String NUMERO_INEXISTENTE = "9999999-99.2099.8.99.9999";

    @Autowired
    private SQLiteProcessoRepository repository;

    @Test
    void deveBuscarProcessoPorNumero() {
        Optional<Processo> resultado = repository.buscarPorNumero(NUMERO);
        assertThat(resultado).isPresent();
        assertThat(resultado.get().numero()).isEqualTo(NUMERO);
        assertThat(resultado.get().classe()).isEqualTo("Procedimento Comum Cível");
    }

    @Test
    void deveRetornarVazioParaProcessoInexistente() {
        Optional<Processo> resultado = repository.buscarPorNumero(NUMERO_INEXISTENTE);
        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarPartes() {
        List<Parte> partes = repository.buscarPartes(NUMERO);
        assertThat(partes).hasSize(2);
        assertThat(partes).extracting(Parte::nome)
                .containsExactlyInAnyOrder("João da Silva", "Empresa XYZ Ltda");
    }

    @Test
    void deveRetornarListaVaziaParaPartesDeProcessoInexistente() {
        List<Parte> partes = repository.buscarPartes(NUMERO_INEXISTENTE);
        assertThat(partes).isEmpty();
    }

    @Test
    void deveBuscarAdvogados() {
        List<Advogado> advogados = repository.buscarAdvogados(NUMERO);
        assertThat(advogados).hasSize(1);
        assertThat(advogados.getFirst().nome()).isEqualTo("Dr. João Advogado");
        assertThat(advogados.getFirst().oab()).isEqualTo("OAB/SC 12.345");
    }

    @Test
    void deveBuscarMagistrados() {
        List<Magistrado> magistrados = repository.buscarMagistrados(NUMERO);
        assertThat(magistrados).hasSize(1);
        assertThat(magistrados.getFirst().nome()).isEqualTo("Juiz Teste Silva");
    }

    @Test
    void deveBuscarDocumentosComPaginacao() {
        List<Documento> docs = repository.buscarDocumentos(NUMERO, 0, 10);
        assertThat(docs).hasSize(3);
    }

    @Test
    void deveRespeitarPaginacaoDeDocumentos() {
        List<Documento> pagina0 = repository.buscarDocumentos(NUMERO, 0, 2);
        List<Documento> pagina1 = repository.buscarDocumentos(NUMERO, 1, 2);
        assertThat(pagina0).hasSize(2);
        assertThat(pagina1).hasSize(1);
    }

    @Test
    void deveContarDocumentos() {
        long total = repository.contarDocumentos(NUMERO);
        assertThat(total).isEqualTo(3L);
    }

    @Test
    void deveBuscarMovimentacoesComPaginacao() {
        List<Movimentacao> movs = repository.buscarMovimentacoes(NUMERO, 0, 10);
        assertThat(movs).hasSize(3);
    }

    @Test
    void deveContarMovimentacoes() {
        long total = repository.contarMovimentacoes(NUMERO);
        assertThat(total).isEqualTo(3L);
    }

    @Test
    void deveBuscarDecisao() {
        Optional<Decisao> decisao = repository.buscarDecisao(NUMERO);
        assertThat(decisao).isPresent();
        assertThat(decisao.get().tipo()).isEqualTo("Sentença");
    }

    @Test
    void deveRetornarVazioParaDecisaoDeProcessoInexistente() {
        Optional<Decisao> decisao = repository.buscarDecisao(NUMERO_INEXISTENTE);
        assertThat(decisao).isEmpty();
    }

    @Test
    void sqlInjectionNaoDeveRetornarResultado() {
        Optional<Processo> result1 = repository.buscarPorNumero("' OR '1'='1");
        assertThat(result1).isEmpty();

        Optional<Processo> result2 = repository.buscarPorNumero("0000001-02.2019.8.99.9018'; DROP TABLE processo; --");
        assertThat(result2).isEmpty();
    }

    @Test
    void deveBuscarSentencaComTexto() {
        Optional<DocumentoTexto> sentenca = repository.buscarSentencaComTexto(NUMERO);
        assertThat(sentenca).isPresent();
        assertThat(sentenca.get().tipo()).isEqualTo("Sentença");
        assertThat(sentenca.get().texto()).isNotBlank();
    }

    @Test
    void deveBuscarPeticaoInicialComTexto() {
        Optional<DocumentoTexto> peticao = repository.buscarPeticaoInicialComTexto(NUMERO);
        assertThat(peticao).isPresent();
        assertThat(peticao.get().tipo()).isEqualTo("Petição Inicial");
        assertThat(peticao.get().texto()).isNotBlank();
    }

    @Test
    void deveBuscarDocumentoPorId() {
        Optional<DocumentoTexto> doc = repository.buscarDocumentoPorId(NUMERO, 1L);
        assertThat(doc).isPresent();
        assertThat(doc.get().texto()).isNotBlank();
    }

    @Test
    void deveRetornarVazioParaDocumentoDeOutroProcesso() {
        Optional<DocumentoTexto> doc = repository.buscarDocumentoPorId(NUMERO_INEXISTENTE, 1L);
        assertThat(doc).isEmpty();
    }

    @Test
    void deveBuscarTodasMovimentacoes() {
        List<Movimentacao> movs = repository.buscarTodasMovimentacoes(NUMERO);
        assertThat(movs).hasSize(3);
        assertThat(movs.get(0).dataMov()).isLessThanOrEqualTo(movs.get(1).dataMov());
    }
}
