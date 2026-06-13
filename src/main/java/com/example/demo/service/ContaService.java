package com.example.demo.service;

import com.example.demo.dto.ContaResponse;
import com.example.demo.dto.CriarContaRequest;
import com.example.demo.dto.TransacaoResponse;
import com.example.demo.dto.TransferenciaRequest;
import com.example.demo.entity.Conta;
import com.example.demo.entity.Transacao;
import com.example.demo.entity.Usuario;
import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.exception.TransferenciaInvalidaException;
import com.example.demo.repository.ContaRepository;
import com.example.demo.repository.TransacaoRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransacaoRepository transacaoRepository;

    public ContaService(
            ContaRepository contaRepository,
            UsuarioRepository usuarioRepository,
            TransacaoRepository transacaoRepository
    ) {
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional
    public ContaResponse criarConta(CriarContaRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário com id " + request.usuarioId() + " não encontrado"
                ));

        Conta conta = new Conta(usuario, request.saldoInicial());
        Conta contaSalva = contaRepository.save(conta);

        return ContaResponse.fromEntity(contaSalva);
    }

    @Transactional(readOnly = true)
    public ContaResponse consultarSaldo(Long contaId) {
        Conta conta = buscarContaOuFalhar(contaId);
        return ContaResponse.fromEntity(conta);
    }

    @Transactional
    public void transferir(TransferenciaRequest request) {
        if (request.contaOrigemId().equals(request.contaDestinoId())) {
            throw new TransferenciaInvalidaException(
                    "Conta de origem e destino não podem ser iguais (id: " + request.contaOrigemId() + ")");
        }

        Conta contaOrigem = buscarContaOuFalhar(request.contaOrigemId());
        Conta contaDestino = buscarContaOuFalhar(request.contaDestinoId());

        contaOrigem.debitar(request.valor());
        contaDestino.creditar(request.valor());

        contaRepository.save(contaOrigem);
        contaRepository.save(contaDestino);

        Transacao transacao = new Transacao(contaOrigem, contaDestino, request.valor());
        transacaoRepository.save(transacao);

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

}
