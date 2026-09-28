package br.jus.tjsc.ai.agent.minuta.domain;

public enum VersaoTipo {

    PROCEDENTE("procedente",
            "Elabore a fundamentação concluindo que os pedidos do autor devem ser ACOLHIDOS. " +
            "Identifique os elementos probatórios que sustentam essa conclusão."),
    IMPROCEDENTE("improcedente",
            "Elabore a fundamentação concluindo que os pedidos do autor devem ser REJEITADOS. " +
            "Identifique os elementos que afastam os pedidos."),
    PARCIALMENTE_PROCEDENTE("parcialmente procedente",
            "Elabore a fundamentação concluindo que PARTE dos pedidos é procedente. " +
            "Especifique quais pedidos são acolhidos e quais são rejeitados, com fundamentação para cada.");

    private final String label;
    private final String instrucaoFundamentacao;

    VersaoTipo(String label, String instrucaoFundamentacao) {
        this.label = label;
        this.instrucaoFundamentacao = instrucaoFundamentacao;
    }

    public String label() { return label; }
    public String instrucaoFundamentacao() { return instrucaoFundamentacao; }

    public static VersaoTipo fromLabel(String label) {
        for (VersaoTipo tipo : values()) {
            if (tipo.label.equalsIgnoreCase(label)) return tipo;
        }
        return PARCIALMENTE_PROCEDENTE;
    }
}
