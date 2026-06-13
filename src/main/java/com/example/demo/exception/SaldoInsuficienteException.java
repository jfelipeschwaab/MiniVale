package com.example.demo.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    private final Long contaId;
    private final BigDecimal saldoAtual;
    private final BigDecimal valorSolicitado;

    public SaldoInsuficienteException(Long contaId, BigDecimal saldoAtual, BigDecimal valorSolicitado) {
        super("Saldo insuficiente na conta " + contaId
                + ": saldo atual " + saldoAtual
                + ", valor solicitado " + valorSolicitado);
        this.contaId = contaId;
        this.saldoAtual = saldoAtual;
        this.valorSolicitado = valorSolicitado;
    }

    public Long getContaId() {
        return contaId;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public BigDecimal getValorSolicitado() {
        return valorSolicitado;
    }


}
