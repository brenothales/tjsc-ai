package br.jus.tjsc.ai.mcp;

public enum TipoDocumento {
    DESPACHO("Despacho"),
    CERTIDAO("Certidão"),
    PETICAO_INICIAL("Petição Inicial"),
    ATA_DE_AUDIENCIA("Ata de Audiência"),
    CONTESTACAO("Contestação"),
    SENTENCA("Sentença"),
    REPLICA("Réplica"),
    DECISAO_DE_SANEAMENTO("Decisão de Saneamento"),
    DECISAO_INTERLOCUTORIA("Decisão Interlocutória"),
    MANIFESTACAO_SOBRE_PROVAS("Manifestação sobre Provas"),
    PETICAO_DE_ACORDO("Petição de Acordo"),
    SENTENCA_HOMOLOGATORIA("Sentença Homologatória de Acordo"),
    PARECER_MINISTERIO_PUBLICO("Parecer do Ministério Público"),
    EXCECAO_PRE_EXECUTIVIDADE("Exceção de Pré-Executividade"),
    EMENDA_A_INICIAL("Emenda à Inicial"),
    EMBARGOS_EXECUCAO_FISCAL("Embargos à Execução Fiscal"),
    EMBARGOS_DECLARACAO("Embargos de Declaração"),
    APELACAO("Apelação"),
    CONTRARRAZOES_APELACAO("Contrarrazões de Apelação");

    private final String valor;

    TipoDocumento(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static String listar() {
        StringBuilder sb = new StringBuilder();
        for (TipoDocumento t : values()) {
            sb.append("'").append(t.valor).append("', ");
        }
        return sb.substring(0, sb.length() - 2);
    }
}
