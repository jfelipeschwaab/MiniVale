package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.Conta;
import com.example.demo.entity.Transacao;
import com.example.demo.entity.Usuario;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.UsuarioJaPossuiContaException;
import com.example.demo.exception.TransferenciaInvalidaException;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransacaoRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.TransferenciaPendente;
import com.example.demo.exception.OtpInvalidoException;
import com.example.demo.repository.TransferenciaPendenteRepository;

import java.time.Instant;
import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransacaoRepository transacaoRepository;
    private final TransferenciaPendenteRepository transferenciaPendenteRepository;
    private final OtpService otpService;

    public ContaService(
            ContaRepository contaRepository,
            UsuarioRepository usuarioRepository,
            TransacaoRepository transacaoRepository,
            TransferenciaPendenteRepository transferenciaPendenteRepository,
            OtpService otpService
    ) {
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoRepository = transacaoRepository;
        this.transferenciaPendenteRepository = transferenciaPendenteRepository;
        this.otpService = otpService;
    }

    @Transactional
    public ContaResponse criarConta(CriarContaRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário com id " + request.usuarioId() + " não encontrado"
                ));

        if (contaRepository.findByUsuarioId(request.usuarioId()).isPresent()) {
            throw new UsuarioJaPossuiContaException(request.usuarioId());
        }

        Conta conta = new Conta(usuario, request.saldoInicial());
        Conta contaSalva = contaRepository.save(conta);

        return ContaResponse.fromEntity(contaSalva);
    }

    @Transactional(readOnly = true)
    public ContaResponse consultarSaldo(Long contaId) {
        Conta conta = buscarContaOuFalhar(contaId);
        return ContaResponse.fromEntity(conta);
    }


    @Transactional(readOnly = true)
    public List<TransacaoResponse> listarExtrato(Long contaId) {
        buscarContaOuFalhar(contaId);

        return transacaoRepository
                .findByContaOrigemIdOrContaDestinoIdOrderByDataHoraDesc(contaId, contaId)
                .stream()
                .map(transacao -> TransacaoResponse.fromEntity(transacao, contaId))
                .toList();
    }

    private Conta buscarContaOuFalhar(Long contaId) {
        return contaRepository.findById(contaId)
                .orElseThrow( () -> new RecursoNaoEncontradoException(
                        "Conta com id " + contaId + " não encontrada"
                ));
    }

    @Transactional
    public TransferenciaPendenteResponse criarTransferenciaPendente(TransferenciaRequest request) {
        if(request.contaOrigemId().equals(request.contaDestinoId())) {
            throw new TransferenciaInvalidaException(
                    "Conta de origem e destino não podem ser iguais (id: " + request.contaOrigemId() + ")"
            );
        }

        Conta contaOrigem = buscarContaOuFalhar(request.contaOrigemId());
        Conta contaDestino = buscarContaOuFalhar(request.contaDestinoId());

        String codigoOtp = otpService.gerarCodigo();
        Instant expiraEm = otpService.calcularExpiracao();

        TransferenciaPendente pendente = new TransferenciaPendente(
                contaOrigem, contaDestino, request.valor(), otpService.hash(codigoOtp), expiraEm
        );
        TransferenciaPendente pendenteSalva = transferenciaPendenteRepository.save(pendente);

        return new TransferenciaPendenteResponse(pendenteSalva.getId(), expiraEm, codigoOtp);
    }

    @Transactional
    public TransacaoResponse confirmarTransferencia(Long transferenciaPendenteId, ConfirmarOtpRequest request) {
        TransferenciaPendente pendente = transferenciaPendenteRepository.findById(transferenciaPendenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Transferência pendente com id " + transferenciaPendenteId + " não encontrada"
                ));

        if(pendente.isConfirmada()) {
            throw new OtpInvalidoException("Transferência já foi confirmada");
        }

        if (pendente.isExpirada()) {
            throw new OtpInvalidoException("Código OTP expirado");
        }

        if (!otpService.isCodigoValido(request.codigo(), pendente.getCodigoOtpHash())) {
            throw new OtpInvalidoException("Código OTP inválido");
        }

        Conta contaOrigem = pendente.getContaOrigem();
        Conta contaDestino = pendente.getContaDestino();

        contaOrigem.debitar(pendente.getValor());
        contaDestino.creditar(pendente.getValor());

        contaRepository.save(contaOrigem);
        contaRepository.save(contaDestino);

        Transacao transacao = new Transacao(contaOrigem, contaDestino, pendente.getValor());
        transacaoRepository.save(transacao);

        pendente.confirmar();
        transferenciaPendenteRepository.save(pendente);

        return TransacaoResponse.fromEntity(transacao, contaOrigem.getId());
    }

}
