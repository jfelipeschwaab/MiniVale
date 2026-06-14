package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean revogado;

    protected RefreshToken() {

    }

    public RefreshToken(String token, Usuario usuario, Instant expiraEm) {
        this.token = token;
        this.usuario = usuario;
        this.expiraEm = expiraEm;
        this.revogado = false;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean isRevogado() {
        return revogado;
    }

    public boolean isValido() {
        return !revogado && Instant.now().isBefore(expiraEm);
    }

    public void revogar() {
        this.revogado = true;
    }
}
