package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;


@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(columnNames = "chave")
)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false)
    private String requestHash;

    @Column(nullable = false)
    private Integer statusCode;

    @Lob
    @Column(nullable = false)
    private String responseBody;

    @Column(nullable = false)
    private Instant criadoEm;

    protected IdempotencyKey() {

    }

    public IdempotencyKey(String chave, String requestHash, Integer statusCode, String responseBody) {
        this.chave = chave;
        this.requestHash = requestHash;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.criadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getChave() {
        return chave;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
