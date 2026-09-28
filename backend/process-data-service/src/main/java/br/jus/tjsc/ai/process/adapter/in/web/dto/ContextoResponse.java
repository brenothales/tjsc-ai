package br.jus.tjsc.ai.process.adapter.in.web.dto;

import br.jus.tjsc.ai.process.application.service.ContextoService.ContextoProcesso;
import br.jus.tjsc.ai.process.domain.model.SituacaoNormalizer;

import java.util.List;

public record ContextoResponse(
        ProcessoDto processo,
        String situacaoNormalizada,
        UnidadeDto unidade,
        List<ParteDto> partes,
        List<AdvogadoDto> advogados,
        List<MagistradoDto> magistrados,
        List<MovimentacaoDto> movimentacoes,
        DocumentoTextoDto sentenca,
        DocumentoTextoDto peticaoInicial
) {
    public static ContextoResponse from(ContextoProcesso ctx) {
        return new ContextoResponse(
                ProcessoDto.from(ctx.processo()),
                SituacaoNormalizer.normalizar(ctx.processo().situacao()),
                ctx.unidade() != null ? UnidadeDto.from(ctx.unidade()) : null,
                ctx.partes().stream().map(ParteDto::from).toList(),
                ctx.advogados().stream().map(AdvogadoDto::from).toList(),
                ctx.magistrados().stream().map(MagistradoDto::from).toList(),
                ctx.movimentacoes().stream().map(MovimentacaoDto::from).toList(),
                ctx.sentenca() != null ? DocumentoTextoDto.from(ctx.sentenca()) : null,
                ctx.peticaoInicial() != null ? DocumentoTextoDto.from(ctx.peticaoInicial()) : null
        );
    }
}
