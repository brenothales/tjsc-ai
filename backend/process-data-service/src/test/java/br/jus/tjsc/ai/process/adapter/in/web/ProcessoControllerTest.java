package br.jus.tjsc.ai.process.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ProcessoControllerTest {

    private static final String NUMERO = "0000001-02.2019.8.99.9018";
    private static final String NUMERO_INEXISTENTE = "9999999-99.2099.8.99.9999";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void deveBuscarProcessoExistente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.processo.numero").value(NUMERO))
                .andExpect(jsonPath("$.processo.classe").value("Procedimento Comum Cível"));
    }

    @Test
    void deveRetornar404ParaProcessoInexistente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", NUMERO_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void deveRetornar400ParaNumeroInvalido() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", "numero-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROCESS_NUMBER"));
    }

    @Test
    void deveBuscarPartes() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/partes", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void deveBuscarAdvogados() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/advogados", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data[0].oab").value("OAB/SC 12.345"));
    }

    @Test
    void deveBuscarMagistrados() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/magistrados", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data[0].nome").value("Juiz Teste Silva"));
    }

    @Test
    void deveBuscarDocumentosComPaginacaoPadrao() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void deveRejeitarSizeMaiorQue100() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos", NUMERO)
                        .param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarPageNegativo() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/movimentacoes", NUMERO)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveBuscarMovimentacoes() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/movimentacoes", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void deveBuscarDecisao() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/decisao", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.decisao.tipo").value("Sentença"));
    }

    @Test
    void deveRetornar404EmSubrecursoParaProcessoInexistente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/partes", NUMERO_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
    }

    @Test
    void sqlInjectionNaoDeveVazarErroInterno() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", "' OR '1'='1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROCESS_NUMBER"));
    }

    @Test
    void dropTableComSufixoDeveRetornarRespostaValida() throws Exception {
        // "0000001-02.2019.8.99.9018'; DROP TABLE processo; --" contém 20 dígitos CNJ válidos.
        // O normalizer extrai o número e a query é executada parametricamente — sem SQL injection.
        // Retorna 200 (processo existe no banco de teste) sem nenhum erro SQL.
        mockMvc.perform(get("/v1/api/processos/{numero}",
                        "0000001-02.2019.8.99.9018'; DROP TABLE processo; --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true));
    }

    @Test
    void naoDeveExporStack() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", "invalido"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void evidencesDeveEstarPresente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", NUMERO))
                .andExpect(jsonPath("$.evidence").exists())
                .andExpect(jsonPath("$.evidence.type").value("PROCESSO"));
    }

    @Test
    void deveBuscarSentencaComTexto() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/sentenca", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.documento.tipo").value("Sentença"))
                .andExpect(jsonPath("$.documento.texto").isNotEmpty());
    }

    @Test
    void deveBuscarPeticaoInicialComTexto() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/peticao", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.documento.tipo").value("Petição Inicial"))
                .andExpect(jsonPath("$.documento.texto").isNotEmpty());
    }

    @Test
    void deveBuscarDocumentoPorId() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos/{id}", NUMERO, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.documento.texto").isNotEmpty());
    }

    @Test
    void deveRetornar404ParaDocumentoInexistente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos/{id}", NUMERO, 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveBuscarContextoCompleto() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/contexto", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processo.numero").value(NUMERO))
                .andExpect(jsonPath("$.situacaoNormalizada").exists())
                .andExpect(jsonPath("$.unidade.vara").exists())
                .andExpect(jsonPath("$.partes").isArray())
                .andExpect(jsonPath("$.advogados").isArray())
                .andExpect(jsonPath("$.magistrados").isArray())
                .andExpect(jsonPath("$.movimentacoes").isArray())
                .andExpect(jsonPath("$.sentenca.texto").isNotEmpty())
                .andExpect(jsonPath("$.peticaoInicial.texto").isNotEmpty());
    }

    @Test
    void situacaoDeveLicitarNormalizada() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}", NUMERO))
                .andExpect(jsonPath("$.processo.situacaoNormalizada").value("JULGADO"));
    }

    @Test
    void contextoDeveRetornar404ParaProcessoInexistente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/contexto", NUMERO_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
    }

    @Test
    void deveBuscarDocumentosComTextoSemFiltro() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos/texto", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void deveBuscarDocumentosComTextoFiltradoPorTipo() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos/texto", NUMERO)
                        .param("tipo", "Sentença"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data[0].tipo").value("Sentença"))
                .andExpect(jsonPath("$.data[0].texto").isNotEmpty());
    }

    @Test
    void deveBuscarDocumentosComTextoRetornaVazioParaTipoInexistente() throws Exception {
        mockMvc.perform(get("/v1/api/processos/{numero}/documentos/texto", NUMERO)
                        .param("tipo", "Tipo Inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void devePesquisarProcessosPorParte() throws Exception {
        mockMvc.perform(get("/v1/api/processos/pesquisar")
                        .param("parte", "João"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void devePesquisarProcessosPorComarca() throws Exception {
        mockMvc.perform(get("/v1/api/processos/pesquisar")
                        .param("comarca", "Florianópolis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.data[0].comarca").value("Florianópolis"));
    }

    @Test
    void devePesquisarProcessosSemResultado() throws Exception {
        mockMvc.perform(get("/v1/api/processos/pesquisar")
                        .param("parte", "Nome Que Nao Existe XYZXYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
