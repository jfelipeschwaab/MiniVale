package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class OtpService {

    private static final int LIMITE_SUPERIOR = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;
    private final long expiracaoOtpMs;

    public OtpService(PasswordEncoder passwordEncoder, @Value("${otp.expiracao-ms}") long expiracaoOtpMs) {
        this.passwordEncoder = passwordEncoder;
        this.expiracaoOtpMs = expiracaoOtpMs;
    }

    public String gerarCodigo() {
        int numero = secureRandom.nextInt(LIMITE_SUPERIOR);
        return String.format("%06d", numero);
    }

    public String hash(String codigo) {
        return passwordEncoder.encode(codigo);
    }

    public boolean isCodigoValido(String codigoDigitado, String codigoHash) {
        return passwordEncoder.matches(codigoDigitado, codigoHash);
    }

    public Instant calcularExpiracao() {
        return Instant.now().plusMillis(expiracaoOtpMs);
    }
}
