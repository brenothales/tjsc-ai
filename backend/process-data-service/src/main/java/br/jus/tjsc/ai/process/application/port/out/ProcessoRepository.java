package br.jus.tjsc.ai.process.application.port.out;

import br.jus.tjsc.ai.process.domain.model.*;

import java.util.List;
import java.util.Optional;

public interface ProcessoRepository {

    Optional<Processo> buscarPorNumero(String numero);

    List<Parte> buscarPartes(String numero);

    List<Advogado> buscarAdvogados(String numero);

    List<Magistrado> buscarMagistrados(String numero);

    List<Documento> buscarDocumentos(String numero, int page, int size);

    long contarDocumentos(String numero);

    List<Movimentacao> buscarMovimentacoes(String numero, int page, int size);

    long contarMovimentacoes(String numero);

    Optional<Decisao> buscarDecisao(String numero);

    Optional<DocumentoTexto> buscarDocumentoPorId(String numero, Long documentoId);

    Optional<DocumentoTexto> buscarSentencaComTexto(String numero);

    Optional<DocumentoTexto> buscarPeticaoInicialComTexto(String numero);

    List<Movimentacao> buscarTodasMovimentacoes(String numero);

    Optional<Unidade> buscarUnidade(Long unidadeId);

    List<Documento> buscarDocumentosFiltrados(String numero, String tipo, int page, int size);

    long contarDocumentosFiltrados(String numero, String tipo);

    List<DocumentoTexto> buscarDocumentosComTexto(String numero, String tipo, int page, int size);

    long contarDocumentosComTexto(String numero, String tipo);

    List<Processo> pesquisarProcessos(String parte, String classe, String comarca, String situacao, int page, int size);

    long contarPesquisaProcessos(String parte, String classe, String comarca, String situacao);
}
