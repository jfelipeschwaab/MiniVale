package com.example.demo.security;

import com.example.demo.entity.Role;
import com.example.demo.entity.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // HS512 exige uma chave de no mínimo 64 bytes (512 bits)
    private static final String SECRET = "0123456789".repeat(7);

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 1000 * 60 * 15);
    }

    @Test
    void gerarAccessToken_deveGerarTokenValidoComEmailEAuthorities() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.ADMIN);

        //Act
        String token = jwtService.gerarAccessToken(usuario);

        //Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extrairEmail(token)).isEqualTo("ana.silva@email.com");
        assertThat(jwtService.isTokenValido(token, usuario)).isTrue();
    }

    @Test
    void isTokenValido_deveRetornarFalse_quandoEmailNaoCorresponde() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);
        Usuario outroUsuario = new Usuario("Bruno Costa", "bruno.costa@email.com", "hash", Role.USER);

        //Act
        String token = jwtService.gerarAccessToken(usuario);

        //Assert
        assertThat(jwtService.isTokenValido(token, outroUsuario)).isFalse();
    }

    @Test
    void isTokenValido_deveLancarExpiredJwtException_quandoTokenExpirado() throws InterruptedException {
        //Arrange
        JwtService jwtServiceComExpiracaoCurta = new JwtService(SECRET, 1L);
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);

        //Act
        String token = jwtServiceComExpiracaoCurta.gerarAccessToken(usuario);
        Thread.sleep(10);

        //Assert
        // O parser do jjwt já lança ExpiredJwtException ao tentar ler as claims
        // de um token expirado. O JwtAuthenticationFilter trata essa exceção
        // (catch JwtException) tratando o token como "não autenticado".
        assertThatThrownBy(() -> jwtServiceComExpiracaoCurta.isTokenValido(token, usuario))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
