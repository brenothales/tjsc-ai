package br.jus.tjsc.ai.process.application.service;

import br.jus.tjsc.ai.process.application.port.out.ProcessoRepository;
import br.jus.tjsc.ai.process.domain.exception.NumeroProcessoInvalidoException;
import br.jus.tjsc.ai.process.domain.model.NumeroProcessoNormalizer;
import br.jus.tjsc.ai.process.domain.model.Processo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProcessoServiceTest {

    private ProcessoRepository repository;
    private ProcessoService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ProcessoRepository.class);
        service = new ProcessoService(repository, new NumeroProcessoNormalizer());
    }

    @Test
    void deveBuscarProcessoNormalizandoNumero() {
        Processo processo = new Processo(1L, "0000001-02.2019.8.99.9018",
                "Classe", "Assunto", "Comarca", 1L, 1L,
                "2019-01-01", null, null, "ativo");
        when(repository.buscarPorNumero("0000001-02.2019.8.99.9018")).thenReturn(Optional.of(processo));

        Optional<Processo> resultado = service.buscar("Processo 0000001-02.2019.8.99.9018");

        assertThat(resultado).isPresent();
        verify(repository).buscarPorNumero("0000001-02.2019.8.99.9018");
    }

    @Test
    void deveRetornarVazioQuandoNaoEncontrado() {
        when(repository.buscarPorNumero(anyString())).thenReturn(Optional.empty());

        Optional<Processo> resultado = service.buscar("0000001-02.2019.8.99.9018");

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveLancarExcecaoParaNumeroInvalido() {
        assertThatThrownBy(() -> service.buscar("numero-invalido"))
                .isInstanceOf(NumeroProcessoInvalidoException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void deveLancarExcecaoParaSqlInjection() {
        assertThatThrownBy(() -> service.buscar("' OR '1'='1"))
                .isInstanceOf(NumeroProcessoInvalidoException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void deveNormalizarNumeroComDigitos() {
        when(repository.buscarPorNumero("0000001-02.2019.8.99.9018")).thenReturn(Optional.empty());

        service.buscar("00000010220198999018");

        verify(repository).buscarPorNumero("0000001-02.2019.8.99.9018");
    }
}
