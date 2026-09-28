package br.jus.tjsc.ai.process.domain.exception;

public class ProcessoNaoEncontradoException extends RuntimeException {

    private final String numeroProcesso;

    public ProcessoNaoEncontradoException(String numeroProcesso) {
        super("Processo não encontrado nos dados disponíveis: " + numeroProcesso);
        this.numeroProcesso = numeroProcesso;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }
}
