package br.jus.tjsc.ai.process.adapter.out.sqlite;

import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.model.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class SQLiteProcessoRepository implements ProcessoRepository {

    private static final Pattern OAB_PATTERN =
            Pattern.compile("^(.+?)\\s+[—–-]\\s+OAB/([A-Z]{2}\\s+[\\d.]+)$");

    private static final Set<String> TIPOS_DECISAO = Set.of(
            "Sentença",
            "Sentença Homologatória de Acordo",
            "Decisão Interlocutória",
            "Decisão de Saneamento"
    );

    private static final Set<String> TIPOS_PEDIDO = Set.of(
            "Petição Inicial",
            "Emenda à Inicial",
            "Petição de Acordo"
    );

    private final NamedParameterJdbcTemplate jdbc;

    public SQLiteProcessoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Processo> buscarPorNumero(String numero) {
        String sql = """
                SELECT p.id, p.numero, p.classe, p.assunto, p.comarca,
                       p.unidade_id, p.magistrado_id, p.dt_aut, p.data_sentenca,
                       p.valor_causa, p.situacao
                FROM processo p
                WHERE p.numero = :numero
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);

        List<Processo> result = jdbc.query(sql, params, (rs, rowNum) -> new Processo(
                rs.getLong("id"),
                rs.getString("numero"),
                rs.getString("classe"),
                rs.getString("assunto"),
                rs.getString("comarca"),
                rs.getObject("unidade_id") != null ? rs.getLong("unidade_id") : null,
                rs.getObject("magistrado_id") != null ? rs.getLong("magistrado_id") : null,
                rs.getString("dt_aut"),
                rs.getString("data_sentenca"),
                rs.getObject("valor_causa") != null
                        ? BigDecimal.valueOf(rs.getDouble("valor_causa")) : null,
                rs.getString("situacao")
        ));

        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public List<Parte> buscarPartes(String numero) {
        String sql = """
                SELECT pa.id, pa.nome, pa.tipo, pa.documento, pa.data_cadastro,
                       pp.polo
                FROM parte pa
                JOIN processo_parte pp ON pp.parte_id = pa.id
                JOIN processo pr ON pr.id = pp.processo_id
                WHERE pr.numero = :numero
                ORDER BY pp.polo, pa.nome
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);

        return jdbc.query(sql, params, (rs, rowNum) -> new Parte(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("tipo"),
                rs.getString("documento"),
                rs.getString("data_cadastro"),
                rs.getString("polo")
        ));
    }

    @Override
    public List<Advogado> buscarAdvogados(String numero) {
        String sql = """
                SELECT d.id, d.autor, d.tipo, d.dt_juntada
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND d.autor LIKE '%OAB/%'
                ORDER BY d.dt_juntada
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);

        return jdbc.query(sql, params, (rs, rowNum) -> {
            String autor = rs.getString("autor");
            String[] parsed = parseAutorOab(autor);
            return new Advogado(
                    rs.getLong("id"),
                    parsed[0],
                    parsed[1],
                    rs.getString("tipo"),
                    rs.getString("dt_juntada")
            );
        });
    }

    @Override
    public List<Magistrado> buscarMagistrados(String numero) {
        String sql = """
                SELECT m.id, m.nome, m.unidade_id, m.data_inicio, m.situacao
                FROM magistrado m
                JOIN processo p ON p.magistrado_id = m.id
                WHERE p.numero = :numero
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);

        return jdbc.query(sql, params, (rs, rowNum) -> new Magistrado(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getObject("unidade_id") != null ? rs.getLong("unidade_id") : null,
                rs.getString("data_inicio"),
                rs.getString("situacao")
        ));
    }

    @Override
    public List<Documento> buscarDocumentos(String numero, int page, int size) {
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                ORDER BY d.dt_juntada DESC
                LIMIT :size OFFSET :offset
                """;

        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("size", size)
                .addValue("offset", (long) page * size);

        return jdbc.query(sql, params, (rs, rowNum) -> new Documento(
                rs.getLong("id"),
                rs.getLong("processo_id"),
                rs.getString("tipo"),
                rs.getString("autor"),
                rs.getString("dt_juntada"),
                rs.getObject("num_paginas") != null ? rs.getInt("num_paginas") : null
        ));
    }

    @Override
    public long contarDocumentos(String numero) {
        String sql = """
                SELECT COUNT(d.id)
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<Movimentacao> buscarMovimentacoes(String numero, int page, int size) {
        String sql = """
                SELECT m.id, m.processo_id, m.codigo, m.descricao, m.data_mov
                FROM movimentacao m
                JOIN processo p ON p.id = m.processo_id
                WHERE p.numero = :numero
                ORDER BY m.data_mov DESC
                LIMIT :size OFFSET :offset
                """;

        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("size", size)
                .addValue("offset", (long) page * size);

        return jdbc.query(sql, params, (rs, rowNum) -> new Movimentacao(
                rs.getLong("id"),
                rs.getLong("processo_id"),
                rs.getObject("codigo") != null ? rs.getInt("codigo") : null,
                rs.getString("descricao"),
                rs.getString("data_mov")
        ));
    }

    @Override
    public long contarMovimentacoes(String numero) {
        String sql = """
                SELECT COUNT(m.id)
                FROM movimentacao m
                JOIN processo p ON p.id = m.processo_id
                WHERE p.numero = :numero
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public Optional<Decisao> buscarDecisao(String numero) {
        // Prioriza Sentença, depois Sentença Homologatória, depois Decisão Interlocutória
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND d.tipo IN (:tipos)
                ORDER BY
                    CASE d.tipo
                        WHEN 'Sentença' THEN 1
                        WHEN 'Sentença Homologatória de Acordo' THEN 2
                        WHEN 'Decisão Interlocutória' THEN 3
                        WHEN 'Decisão de Saneamento' THEN 4
                        ELSE 5
                    END,
                    d.dt_juntada DESC
                LIMIT 1
                """;

        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("tipos", TIPOS_DECISAO);

        List<Decisao> result = jdbc.query(sql, params, (rs, rowNum) -> new Decisao(
                rs.getLong("id"),
                rs.getLong("processo_id"),
                rs.getString("tipo"),
                rs.getString("autor"),
                rs.getString("dt_juntada"),
                rs.getObject("num_paginas") != null ? rs.getInt("num_paginas") : null
        ));

        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public Optional<DocumentoTexto> buscarDocumentoPorId(String numero, Long documentoId) {
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas, d.texto
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND d.id = :documentoId
                """;

        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("documentoId", documentoId);

        List<DocumentoTexto> result = jdbc.query(sql, params, this::mapDocumentoTexto);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public Optional<DocumentoTexto> buscarSentencaComTexto(String numero) {
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas, d.texto
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND d.tipo IN ('Sentença', 'Sentença Homologatória de Acordo')
                ORDER BY
                    CASE d.tipo WHEN 'Sentença' THEN 1 ELSE 2 END,
                    d.dt_juntada DESC
                LIMIT 1
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);
        List<DocumentoTexto> result = jdbc.query(sql, params, this::mapDocumentoTexto);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public Optional<DocumentoTexto> buscarPeticaoInicialComTexto(String numero) {
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas, d.texto
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND d.tipo IN ('Petição Inicial', 'Emenda à Inicial')
                ORDER BY
                    CASE d.tipo WHEN 'Petição Inicial' THEN 1 ELSE 2 END,
                    d.dt_juntada ASC
                LIMIT 1
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);
        List<DocumentoTexto> result = jdbc.query(sql, params, this::mapDocumentoTexto);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public List<Movimentacao> buscarTodasMovimentacoes(String numero) {
        String sql = """
                SELECT m.id, m.processo_id, m.codigo, m.descricao, m.data_mov
                FROM movimentacao m
                JOIN processo p ON p.id = m.processo_id
                WHERE p.numero = :numero
                ORDER BY m.data_mov ASC
                """;

        var params = new MapSqlParameterSource().addValue("numero", numero);
        return jdbc.query(sql, params, (rs, rowNum) -> new Movimentacao(
                rs.getLong("id"),
                rs.getLong("processo_id"),
                rs.getObject("codigo") != null ? rs.getInt("codigo") : null,
                rs.getString("descricao"),
                rs.getString("data_mov")
        ));
    }

    @Override
    public Optional<Unidade> buscarUnidade(Long unidadeId) {
        if (unidadeId == null) return Optional.empty();
        String sql = """
                SELECT id, comarca, vara, dt_ini, status
                FROM unidade
                WHERE id = :id
                """;
        var params = new MapSqlParameterSource().addValue("id", unidadeId);
        List<Unidade> result = jdbc.query(sql, params, (rs, rowNum) -> new Unidade(
                rs.getLong("id"),
                rs.getString("comarca"),
                rs.getString("vara"),
                rs.getString("dt_ini"),
                rs.getString("status")
        ));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    public List<Documento> buscarDocumentosFiltrados(String numero, String tipo, int page, int size) {
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND (:tipo IS NULL OR d.tipo = :tipo)
                ORDER BY d.dt_juntada DESC
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("tipo", tipo)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return jdbc.query(sql, params, (rs, rowNum) -> new Documento(
                rs.getLong("id"),
                rs.getLong("processo_id"),
                rs.getString("tipo"),
                rs.getString("autor"),
                rs.getString("dt_juntada"),
                rs.getObject("num_paginas") != null ? rs.getInt("num_paginas") : null
        ));
    }

    @Override
    public long contarDocumentosFiltrados(String numero, String tipo) {
        String sql = """
                SELECT COUNT(d.id)
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND (:tipo IS NULL OR d.tipo = :tipo)
                """;
        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("tipo", tipo);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<DocumentoTexto> buscarDocumentosComTexto(String numero, String tipo, int page, int size) {
        String sql = """
                SELECT d.id, d.processo_id, d.tipo, d.autor, d.dt_juntada, d.num_paginas, d.texto
                FROM documento d
                JOIN processo p ON p.id = d.processo_id
                WHERE p.numero = :numero
                  AND (:tipo IS NULL OR d.tipo = :tipo)
                ORDER BY d.dt_juntada DESC
                LIMIT :size OFFSET :offset
                """;
        var params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("tipo", tipo)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return jdbc.query(sql, params, this::mapDocumentoTexto);
    }

    @Override
    public long contarDocumentosComTexto(String numero, String tipo) {
        return contarDocumentosFiltrados(numero, tipo);
    }

    @Override
    public List<Processo> pesquisarProcessos(String parte, String classe, String comarca, String situacao, int page, int size) {
        String sql = buildPesquisaQuery(false);
        var params = buildPesquisaParams(parte, classe, comarca, situacao)
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        return jdbc.query(sql, params, (rs, rowNum) -> new Processo(
                rs.getLong("id"),
                rs.getString("numero"),
                rs.getString("classe"),
                rs.getString("assunto"),
                rs.getString("comarca"),
                rs.getObject("unidade_id") != null ? rs.getLong("unidade_id") : null,
                rs.getObject("magistrado_id") != null ? rs.getLong("magistrado_id") : null,
                rs.getString("dt_aut"),
                rs.getString("data_sentenca"),
                rs.getObject("valor_causa") != null ? BigDecimal.valueOf(rs.getDouble("valor_causa")) : null,
                rs.getString("situacao")
        ));
    }

    @Override
    public long contarPesquisaProcessos(String parte, String classe, String comarca, String situacao) {
        String sql = buildPesquisaQuery(true);
        var params = buildPesquisaParams(parte, classe, comarca, situacao);
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    private String buildPesquisaQuery(boolean count) {
        String select = count
                ? "SELECT COUNT(DISTINCT p.id)"
                : "SELECT DISTINCT p.id, p.numero, p.classe, p.assunto, p.comarca, p.unidade_id, p.magistrado_id, p.dt_aut, p.data_sentenca, p.valor_causa, p.situacao";
        return select + """
                 FROM processo p
                LEFT JOIN processo_parte pp ON pp.processo_id = p.id
                LEFT JOIN parte pa ON pa.id = pp.parte_id
                WHERE (:parte IS NULL OR pa.nome LIKE :partePattern)
                  AND (:classe IS NULL OR p.classe = :classe)
                  AND (:comarca IS NULL OR p.comarca = :comarca)
                  AND (:situacao IS NULL OR p.situacao LIKE :situacaoPattern)
                """ + (count ? "" : "ORDER BY p.dt_aut DESC LIMIT :size OFFSET :offset");
    }

    private MapSqlParameterSource buildPesquisaParams(String parte, String classe, String comarca, String situacao) {
        return new MapSqlParameterSource()
                .addValue("parte", parte)
                .addValue("partePattern", parte != null ? "%" + parte + "%" : null)
                .addValue("classe", classe)
                .addValue("comarca", comarca)
                .addValue("situacao", situacao)
                .addValue("situacaoPattern", situacao != null ? "%" + situacao + "%" : null);
    }

    private DocumentoTexto mapDocumentoTexto(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DocumentoTexto(
                rs.getLong("id"),
                rs.getLong("processo_id"),
                rs.getString("tipo"),
                rs.getString("autor"),
                rs.getString("dt_juntada"),
                rs.getObject("num_paginas") != null ? rs.getInt("num_paginas") : null,
                rs.getString("texto")
        );
    }

    private String[] parseAutorOab(String autor) {
        if (autor == null) return new String[]{"", ""};
        Matcher m = OAB_PATTERN.matcher(autor.trim());
        if (m.matches()) {
            return new String[]{m.group(1).trim(), "OAB/" + m.group(2).trim()};
        }
        return new String[]{autor, ""};
    }
}
