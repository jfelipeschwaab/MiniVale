package com.example.demo.dto;

import com.example.demo.entity.Conta;

import java.math.BigDecimal;

public record ContaResponse(
        Long id,
        Long usuarioId,
        String nomeUsuario,
        BigDecimal saldo
) {

    public static ContaResponse fromEntity(Conta conta) {
        return new ContaResponse(
                conta.getId(),
                conta.getUsuario().getId(),
                conta.getUsuario().getNome(),
                conta.getSaldo()
        );
    }
}
