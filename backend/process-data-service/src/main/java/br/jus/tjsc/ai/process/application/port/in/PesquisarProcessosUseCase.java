package br.jus.tjsc.ai.process.application.port.in;

import br.jus.tjsc.ai.process.domain.model.Processo;

import java.util.List;

public interface PesquisarProcessosUseCase {
    List<Processo> pesquisar(String parte, String classe, String comarca, String situacao, int page, int size);
    long contar(String parte, String classe, String comarca, String situacao);
}
