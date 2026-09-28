package br.jus.tjsc.ai.mcp;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class ProcessoTools {

    private final ProcessDataClient client;

    ProcessoTools(ProcessDataClient client) {
        this.client = client;
    }

    @McpTool(
        name = "buscar_contexto_completo",
        description = """
            Retorna todos os dados de um processo judicial em uma única chamada: \
            informações principais, vara (unidade), partes (autor/réu), advogados, \
            magistrado, movimentações, sentença com texto integral e petição inicial com texto integral. \
            Use esta ferramenta como ponto de entrada para qualquer pergunta abrangente sobre um processo, \
            como 'o que aconteceu nesse processo?', 'me explique o processo', 'qual foi o resultado?'."""
    )
    public String buscarContextoCompleto(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ (ex: 0000001-02.2019.8.24.0000) ou 20 dígitos contínuos", required = true)
            String numero) {
        return client.buscarContexto(numero);
    }

    @McpTool(
        name = "buscar_processo",
        description = """
            Retorna APENAS os dados básicos de um processo: classe, assunto, comarca, vara, \
            data de autuação, data da sentença, valor da causa e situação (JULGADO, EM_ANDAMENTO, ARQUIVADO). \
            NÃO inclui movimentações, partes, advogados, documentos nem textos. \
            Para esses dados, use as ferramentas específicas: buscar_movimentacoes, buscar_partes, etc."""
    )
    public String buscarProcesso(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarProcesso(numero);
    }

    @McpTool(
        name = "buscar_partes",
        description = """
            Lista as partes do processo: autor(es), réu(s) e outros participantes, \
            com nome, tipo, documento e polo (ativo/passivo). \
            Use quando perguntado sobre 'quem são as partes', 'quem está processando quem', \
            'qual é o autor', 'qual é o réu'."""
    )
    public String buscarPartes(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarPartes(numero);
    }

    @McpTool(
        name = "buscar_advogados",
        description = """
            Lista os advogados identificados nos documentos do processo com nome e número OAB. \
            Use quando perguntado sobre 'qual é o advogado', 'quem representa as partes', \
            'número da OAB'."""
    )
    public String buscarAdvogados(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarAdvogados(numero);
    }

    @McpTool(
        name = "buscar_magistrado",
        description = """
            Retorna o magistrado (juiz/desembargador) responsável pelo processo. \
            Use quando perguntado sobre 'quem é o juiz', 'qual magistrado', \
            'quem assinou a sentença'."""
    )
    public String buscarMagistrado(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarMagistrados(numero);
    }

    @McpTool(
        name = "buscar_movimentacoes",
        description = """
            ÚNICA fonte de movimentações processuais. DEVE ser chamada sempre que o usuário \
            perguntar sobre movimentações, andamentos, histórico ou o que aconteceu no processo — \
            mesmo que dados básicos já tenham sido consultados. \
            Retorna as 50 movimentações mais recentes com data e descrição, ordenadas por data descendente."""
    )
    public String buscarMovimentacoes(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarMovimentacoes(numero, 0, 50);
    }

    @McpTool(
        name = "buscar_documentos",
        description = """
            Lista os documentos do processo (sem texto integral) com tipo, autor e data. \
            Use para saber quais documentos existem antes de solicitar o texto integral. \
            Retorna os 50 documentos mais recentes."""
    )
    public String buscarDocumentos(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarDocumentos(numero, 0, 50);
    }

    @McpTool(
        name = "buscar_documento_por_id",
        description = """
            Retorna o texto integral de um documento específico pelo seu ID. \
            Use quando souber o ID do documento (obtido via buscar_documentos) \
            e precisar do conteúdo completo."""
    )
    public String buscarDocumentoPorId(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero,
            @McpArg(name = "documentoId", description = "ID numérico do documento", required = true)
            Long documentoId) {
        return client.buscarDocumentoPorId(numero, documentoId);
    }

    @McpTool(
        name = "buscar_documentos_com_texto",
        description = """
            Retorna documentos com texto integral, podendo filtrar por tipo. \
            Tipos válidos (use exatamente este texto): \
            'Despacho', 'Certidão', 'Petição Inicial', 'Ata de Audiência', 'Contestação', \
            'Sentença', 'Réplica', 'Decisão de Saneamento', 'Decisão Interlocutória', \
            'Manifestação sobre Provas', 'Petição de Acordo', 'Sentença Homologatória de Acordo', \
            'Parecer do Ministério Público', 'Exceção de Pré-Executividade', 'Emenda à Inicial', \
            'Embargos à Execução Fiscal', 'Embargos de Declaração', 'Apelação', 'Contrarrazões de Apelação'. \
            Omita 'tipo' para retornar todos os documentos com texto. \
            NÃO use buscar_sentenca/buscar_peticao_inicial quando já tiver o tipo aqui. \
            Use com tipo='Despacho' quando o usuário perguntar sobre despachos."""
    )
    public String buscarDocumentosComTexto(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero,
            @McpArg(name = "tipo", description = "Tipo do documento. Ex: 'Contestação', 'Despacho', 'Ata de Audiência'. Deixe em branco para todos.", required = true)
            String tipo) {
        return client.buscarDocumentosComTexto(numero, tipo.isBlank() ? null : tipo, 0, 5);
    }

    @McpTool(
        name = "buscar_sentenca",
        description = """
            Retorna a sentença principal do processo com texto integral. \
            Use quando perguntado sobre 'qual foi a sentença', 'o que diz a sentença', \
            'o processo foi julgado?', 'qual a decisão final?'."""
    )
    public String buscarSentenca(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarSentenca(numero);
    }

    @McpTool(
        name = "buscar_peticao_inicial",
        description = """
            Retorna a petição inicial do processo com texto integral. \
            Use quando perguntado sobre 'qual é o pedido', 'o que pede o autor', \
            'quais são os fatos narrados', 'como o processo foi iniciado'."""
    )
    public String buscarPeticaoInicial(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarPeticaoInicial(numero);
    }

    @McpTool(
        name = "buscar_decisao",
        description = """
            Retorna a decisão principal do processo (sentença, decisão interlocutória ou \
            sentença homologatória de acordo). Use para verificar o tipo e data da decisão \
            sem precisar do texto integral. \
            NÃO inclui despachos — para buscar despachos use buscar_documentos_com_texto com tipo 'Despacho'."""
    )
    public String buscarDecisao(
            @McpArg(name = "numero", description = "Número do processo no formato CNJ", required = true)
            String numero) {
        return client.buscarDecisao(numero);
    }

    @McpTool(
        name = "executar_sql",
        description = """
            Executa uma consulta SELECT diretamente no banco de dados SQLite. \
            Use para responder perguntas analíticas ou agregadas que as outras ferramentas não cobrem, \
            como contagens, totais, rankings e comparações entre processos. \
            Schema exato (use EXATAMENTE estes nomes de colunas): \
            processo(id, numero, classe, assunto, comarca, unidade_id, magistrado_id, dt_aut, data_sentenca, valor_causa, situacao). \
            magistrado(id, nome, unidade_id, data_inicio, situacao). \
            unidade(id, comarca, vara, dt_ini, status). \
            parte(id, nome, tipo, documento, data_cadastro). \
            processo_parte(id, processo_id, parte_id, polo). \
            movimentacao(id, processo_id, codigo, descricao, data_mov). \
            documento(id, processo_id, tipo, autor, dt_juntada, num_paginas, texto). \
            REGRAS CRÍTICAS: \
            1) processo.comarca já contém o nome da comarca — NÃO faça JOIN com unidade apenas para obter comarca. \
            2) situacao tem casing inconsistente — use ALWAYS LOWER(situacao) LIKE '%valor%'. \
            3) Para filtrar por magistrado: JOIN magistrado m ON p.magistrado_id = m.id WHERE LOWER(m.nome) LIKE '%nome%'. \
            4) polo em processo_parte: 'ativo' ou 'passivo' (minúsculo). \
            5) Apenas SELECT é permitido. Limite máximo: 100 linhas."""
    )
    public String executarSql(
            @McpArg(name = "sql", description = "Consulta SELECT válida em SQLite. Ex: SELECT classe, COUNT(*) as total FROM processo GROUP BY classe ORDER BY total DESC", required = true)
            String sql) {
        return client.executarSql(sql);
    }

    @McpTool(
        name = "pesquisar_processos",
        description = """
            Pesquisa processos por filtros combinados: nome de parte, classe processual, \
            comarca e situação. Todos os filtros são opcionais. \
            Busca parcial por nome de parte (ex: 'Silva' encontra 'João da Silva'). \
            Use quando perguntado sobre 'processos do João', 'processos em Florianópolis', \
            'processos de Procedimento Comum Cível', 'quais processos estão julgados'."""
    )
    public String pesquisarProcessos(
            @McpArg(name = "parte", description = "Nome ou parte do nome da parte (autor/réu). Ex: 'Silva'. Envie string vazia se não souber.", required = true)
            String parte,
            @McpArg(name = "classe", description = "Classe processual. Ex: 'Procedimento Comum Cível'. Envie string vazia se não souber.", required = true)
            String classe,
            @McpArg(name = "comarca", description = "Comarca. Ex: 'Florianópolis'. Envie string vazia se não souber.", required = true)
            String comarca,
            @McpArg(name = "situacao", description = "Situação. Ex: 'julgado', 'andamento'. Envie string vazia se não souber.", required = true)
            String situacao) {
        return client.pesquisarProcessos(
                parte.isBlank() ? null : parte,
                classe.isBlank() ? null : classe,
                comarca.isBlank() ? null : comarca,
                situacao.isBlank() ? null : situacao,
                0, 20);
    }
}
