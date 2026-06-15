package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OtpServiceTest {

    private static final long EXPIRACAO_OTP_MS = 300_000L;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(new BCryptPasswordEncoder(), EXPIRACAO_OTP_MS);
    }

    @Test
    void gerarCodigo_deveGerarCodigoNumericoDe6Digitos() {
        //Act
        String codigo = otpService.gerarCodigo();

        //Assert
        assertThat(codigo).hasSize(6);
        assertThat(codigo).matches("\\d{6}");
    }

    @Test
    void hashEIsCodigoValido_devemSerConsistentes() {
        //Arrange
        String codigo = otpService.gerarCodigo();
        String codigoErrado = codigo.equals("000000") ? "111111" : "000000";

        //Act
        String hash = otpService.hash(codigo);

        //Assert
        assertThat(otpService.isCodigoValido(codigo, hash)).isTrue();
        assertThat(otpService.isCodigoValido(codigoErrado, hash)).isFalse();
    }

    @Test
    void calcularExpiracao_deveRetornarInstanteFuturoDentroDoLimiteConfigurado() {
        //Arrange
        Instant antes = Instant.now();

        //Act
        Instant expiracao = otpService.calcularExpiracao();

        //Assert
        assertThat(expiracao).isAfter(antes);
        assertThat(expiracao).isBeforeOrEqualTo(antes.plusMillis(EXPIRACAO_OTP_MS + 1000));
    }
}
