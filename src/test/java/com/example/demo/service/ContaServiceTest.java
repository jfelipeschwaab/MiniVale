package com.example.demo.service;

import com.example.demo.dto.ContaResponse;
import com.example.demo.dto.CriarContaRequest;
import com.example.demo.dto.TransacaoResponse;
import com.example.demo.dto.TransferenciaRequest;
import com.example.demo.entity.Conta;
import com.example.demo.entity.Transacao;
import com.example.demo.entity.Usuario;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.SaldoInsuficienteException;
import com.example.demo.exception.TransferenciaInvalidaException;
import com.example.demo.exception.UsuarioJaPossuiContaException;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransacaoRepository;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContaServiceTest {

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private ContaService contaService;

    @Test
    void criarConta_deveCriarContaComSucesso_quandoUsuarioExiste() {
        //Arrange
        Long usuarioId = 1L;
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@gmail.com");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        CriarContaRequest request = new CriarContaRequest(usuarioId, new BigDecimal("100.00"));

        Conta contaSalva = new Conta(usuario, request.saldoInicial());
        ReflectionTestUtils.setField(contaSalva, "id", 10L);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(contaRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(contaRepository.save(any(Conta.class))).thenReturn(contaSalva);

        //Act
        ContaResponse response = contaService.criarConta(request);

        //Assert
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.usuarioId()).isEqualTo(usuarioId);
        assertThat(response.nomeUsuario()).isEqualTo("Ana Silva");
        assertThat(response.saldo()).isEqualByComparingTo("100.00");

        verify(contaRepository).save(any(Conta.class));
    }

    @Test
    void criarConta_deveLancarExcecao_quandoUsuarioNaoExiste() {
        //Arrange
        Long usuarioId = 99L;
        CriarContaRequest request = new CriarContaRequest(usuarioId, new BigDecimal("100.00"));

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> contaService.criarConta(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");

        verify(contaRepository, never()).save(any());
    }

    @Test
    void criarConta_deveLancarExcecao_quandoUsuarioJaPossuiConta() {
        //Arrange
        Long usuarioId = 1L;
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@gmail.com");
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        CriarContaRequest request = new CriarContaRequest(usuarioId, new BigDecimal("100.00"));

        Conta contaExistente = new Conta(usuario, new BigDecimal("50.00"));
        ReflectionTestUtils.setField(contaExistente, "id", 5L);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(contaRepository.findByUsuarioId(usuarioId)).thenReturn(Optional.of(contaExistente));

        //Act + Assert
        assertThatThrownBy(() -> contaService.criarConta(request))
                .isInstanceOf(UsuarioJaPossuiContaException.class)
                .hasMessageContaining("1");

        verify(contaRepository, never()).save(any());
    }

    @Test
    void consultarSaldo_deveRetornarConta_quandoContaExiste() {
        //Arrange
        Long contaId = 1L;
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com");
        ReflectionTestUtils.setField(usuario, "id", 1L);

        Conta conta = new Conta(usuario, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(conta, "id", contaId);

        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta));

        //Act
        ContaResponse response = contaService.consultarSaldo(contaId);

        //Assert
        assertThat(response.id()).isEqualTo(contaId);
        assertThat(response.saldo()).isEqualByComparingTo("100.00");
    }

    @Test
    void consultarSaldo_deveLancarExcecao_quandoContaNaoExiste() {
        //Arrange
        Long contaId = 99L;
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> contaService.consultarSaldo(contaId))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void transferir_deveTransferirComSucesso_quandoSaldoSuficiente() {
        //Arrange
        Usuario usuarioOrigem = new Usuario("Ana Silva", "ana.silva@email.com");
        ReflectionTestUtils.setField(usuarioOrigem, "id", 1L);
        Conta contaOrigem = new Conta(usuarioOrigem, new BigDecimal("100.00"));
        ReflectionTestUtils.setField(contaOrigem, "id", 1L);

        Usuario usuarioDestino = new Usuario("Bruno Costa", "bruno.costa@email.com");
        ReflectionTestUtils.setField(usuarioDestino, "id", 2L);
        Conta contaDestino = new Conta(usuarioDestino, BigDecimal.ZERO);
        ReflectionTestUtils.setField(contaDestino, "id", 2L);

        TransferenciaRequest request = new TransferenciaRequest(1L, 2L, new BigDecimal("30.00"));

        when(contaRepository.findById(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(2L)).thenReturn(Optional.of(contaDestino));

        //Act
        contaService.transferir(request);

        //Assert
        assertThat(contaOrigem.getSaldo()).isEqualByComparingTo("70.00");
        assertThat(contaDestino.getSaldo()).isEqualByComparingTo("30.00");

        verify(contaRepository).save(contaOrigem);
        verify(contaRepository).save(contaDestino);
        verify(transacaoRepository).save(any(Transacao.class));
    }

    @Test
    void transferir_deveLancarExcecao_quandoContaOrigemIgualContaDestino() {
        //Arrange
        TransferenciaRequest request = new TransferenciaRequest(1L, 1L, new BigDecimal("30.00"));

        //Act + Assert
        assertThatThrownBy(() -> contaService.transferir(request))
                .isInstanceOf(TransferenciaInvalidaException.class);

        verify(contaRepository, never()).findById(any());
    }

    @Test
    void transferir_deveLancarExcecao_quandoContaOrigemNaoExiste() {
        //Arrange
        TransferenciaRequest request = new TransferenciaRequest(1L, 2L, new BigDecimal("30.00"));

        when(contaRepository.findById(1L)).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> contaService.transferir(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("1");

        verify(contaRepository, never()).save(any());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void transferir_deveLancarExcecao_quandoSaldoInsuficiente() {
        //Arrange
        Usuario usuarioOrigem = new Usuario("Ana Silva", "ana.silva@email.com");
        ReflectionTestUtils.setField(usuarioOrigem, "id", 1L);
        Conta contaOrigem = new Conta(usuarioOrigem, new BigDecimal("10.00"));
        ReflectionTestUtils.setField(contaOrigem, "id", 1L);

        Usuario usuarioDestino = new Usuario("Bruno Costa", "bruno.costa@email.com");
        ReflectionTestUtils.setField(usuarioDestino, "id", 2L);
        Conta contaDestino = new Conta(usuarioDestino, BigDecimal.ZERO);
        ReflectionTestUtils.setField(contaDestino, "id", 2L);

        TransferenciaRequest request = new TransferenciaRequest(1L, 2L, new BigDecimal("100.00"));

        when(contaRepository.findById(1L)).thenReturn(Optional.of(contaOrigem));
        when(contaRepository.findById(2L)).thenReturn(Optional.of(contaDestino));

        //Act + Assert
        assertThatThrownBy(() -> contaService.transferir(request))
                .isInstanceOf(SaldoInsuficienteException.class);

        assertThat(contaOrigem.getSaldo()).isEqualByComparingTo("10.00");
        assertThat(contaDestino.getSaldo()).isEqualByComparingTo("0.00");

        verify(contaRepository, never()).save(any());
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void listarExtrato_deveRetornarTransacoesComTiposCorretos() {
        //Arrange
        Long contaId = 1L;

        Usuario usuario1 = new Usuario("Ana Silva", "ana.silva@email.com");
        ReflectionTestUtils.setField(usuario1, "id", 1L);
        Conta conta1 = new Conta(usuario1, new BigDecimal("70.00"));
        ReflectionTestUtils.setField(conta1, "id", contaId);

        Usuario usuario2 = new Usuario("Bruno Costa", "bruno.costa@email.com");
        ReflectionTestUtils.setField(usuario2, "id", 2L);
        Conta conta2 = new Conta(usuario2, new BigDecimal("30.00"));
        ReflectionTestUtils.setField(conta2, "id", 2L);

        Transacao saida = new Transacao(conta1, conta2, new BigDecimal("30.00"));
        ReflectionTestUtils.setField(saida, "id", 1L);

        when(contaRepository.findById(contaId)).thenReturn(Optional.of(conta1));
        when(transacaoRepository.findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId))
                .thenReturn(List.of(saida));

        //Act
        List<TransacaoResponse> extrato = contaService.listarExtrato(contaId);

        //Assert
        assertThat(extrato).hasSize(1);
        assertThat(extrato.get(0).tipo()).isEqualTo(TransacaoResponse.TipoMovimento.SAIDA);
        assertThat(extrato.get(0).valor()).isEqualByComparingTo("30.00");
    }

    @Test
    void listarExtrato_deveLancarExcecao_quandoContaNaoExiste() {
        //Arrange
        Long contaId = 99L;
        when(contaRepository.findById(contaId)).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> contaService.listarExtrato(contaId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(transacaoRepository, never())
                .findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(any(), any());
    }
}
