package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Movimentacao;

import java.util.List;

public interface BuscarMovimentacoesUseCase {
    List<Movimentacao> buscar(String numeroProcesso, int page, int size);
    long contar(String numeroProcesso);
}
