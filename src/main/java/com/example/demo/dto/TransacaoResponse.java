package com.example.demo.dto;

import com.example.demo.entity.Transacao;

import java.math.BigDecimal;
import java.time.Instant;

public record TransacaoResponse(
        Long id,
        Long contaOrigemId,
        Long contaDestinoId,
        BigDecimal valor,
        Instant dataHora,
        TipoMovimento tipo
) {

    public enum TipoMovimento {
        ENTRADA,
        SAIDA
    }

    public static TransacaoResponse fromEntity(Transacao transacao, Long contaReferenciaId) {
        TipoMovimento tipo = transacao.getContaOrigem().getId().equals(contaReferenciaId) ? TipoMovimento.SAIDA : TipoMovimento.ENTRADA;

        return new TransacaoResponse(
                transacao.getId(),
                transacao.getContaOrigem().getId(),
                transacao.getContaDestino().getId(),
                transacao.getValor(),
                transacao.getDataHora(),
                tipo
        );
    }
}
