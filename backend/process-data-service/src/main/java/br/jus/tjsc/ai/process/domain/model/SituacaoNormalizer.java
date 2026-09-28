package br.jus.tjsc.ai.process.domain.model;

public final class SituacaoNormalizer {

    private SituacaoNormalizer() {}

    public static String normalizar(String situacao) {
        if (situacao == null || situacao.isBlank()) return null;
        return switch (situacao.toUpperCase().trim()) {
            case "JULGADO"       -> "JULGADO";
            case "EM ANDAMENTO"  -> "EM_ANDAMENTO";
            case "ARQUIVADO"     -> "ARQUIVADO";
            case "SUSPENSO"      -> "SUSPENSO";
            default              -> situacao.toUpperCase().trim();
        };
    }
}
