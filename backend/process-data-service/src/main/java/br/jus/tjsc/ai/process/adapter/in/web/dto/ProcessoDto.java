package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.domain.model.Processo;
import br.jus.tjsc.ai.process.domain.model.SituacaoNormalizer;

import java.math.BigDecimal;

public record ProcessoDto(
        String numero,
        String classe,
        String assunto,
        String comarca,
        String dataAutuacao,
        String dataSentenca,
        BigDecimal valorCausa,
        String situacao,
        String situacaoNormalizada
) {
    public static ProcessoDto from(Processo p) {
        return new ProcessoDto(
                p.numero(), p.classe(), p.assunto(), p.comarca(),
                p.dataAutuacao(), p.dataSentenca(), p.valorCausa(),
                p.situacao(),
                SituacaoNormalizer.normalizar(p.situacao())
        );
    }
}
