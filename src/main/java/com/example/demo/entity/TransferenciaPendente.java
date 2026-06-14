package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transferencias_pendentes")
public class TransferenciaPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conta_origem_id", nullable = false)
    private Conta contaOrigem;

    @ManyToOne
    @JoinColumn(name = "conta_destino_id", nullable = false)
    private Conta contaDestino;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String codigoOtpHash;

    @Column(nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean confirmada;

    protected TransferenciaPendente() {

    }

    public TransferenciaPendente(Conta contaOrigem, Conta contaDestino, BigDecimal valor, String codigoOtpHash, Instant expiraEm) {
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.codigoOtpHash = codigoOtpHash;
        this.expiraEm = expiraEm;
        this.confirmada = false;
    }

    public Long getId() {
        return id;
    }

    public Conta getContaOrigem() {
        return contaOrigem;
    }

    public Conta getContaDestino() {
        return contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getCodigoOtpHash() {
        return codigoOtpHash;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean isConfirmada() {
        return confirmada;
    }

    public boolean isExpirada() {
        return Instant.now().isAfter(expiraEm);
    }

    public void confirmar() {
        this.confirmada = true;
    }
}