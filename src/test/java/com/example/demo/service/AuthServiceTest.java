package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.Usuario;
import com.example.demo.exception.TokenInvalidoException;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private static final long EXPIRACAO_REFRESH_TOKEN_MS = 604_800_000L;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // AuthService recebe "jwt.expiracao-refresh-ms" via @Value, então o Mockito
        // não consegue resolver esse parâmetro com @InjectMocks (não é um @Mock).
        // Por isso instanciamos manualmente, passando o valor explicitamente.
        authService = new AuthService(authenticationManager, refreshTokenRepository, jwtService, EXPIRACAO_REFRESH_TOKEN_MS);
    }

    @Test
    void login_deveRetornarTokens_quandoCredenciaisValidas() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);
        ReflectionTestUtils.setField(usuario, "id", 1L);

        LoginRequest request = new LoginRequest("ana.silva@email.com", "senha123");

        Authentication authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.gerarAccessToken(usuario)).thenReturn("access-token");

        //Act
        TokenResponse response = authService.login(request);

        //Assert
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_devePropagarExcecao_quandoCredenciaisInvalidas() {
        //Arrange
        LoginRequest request = new LoginRequest("ana.silva@email.com", "senhaErrada");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        //Act + Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_deveGerarNovosTokensERevogarAntigo_quandoRefreshTokenValido() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);
        ReflectionTestUtils.setField(usuario, "id", 1L);

        RefreshToken refreshTokenSalvo = new RefreshToken("token-antigo", usuario, Instant.now().plusSeconds(3600));

        RefreshTokenRequest request = new RefreshTokenRequest("token-antigo");

        when(refreshTokenRepository.findByToken("token-antigo")).thenReturn(Optional.of(refreshTokenSalvo));
        when(jwtService.gerarAccessToken(usuario)).thenReturn("novo-access-token");

        //Act
        TokenResponse response = authService.refresh(request);

        //Assert
        assertThat(response.accessToken()).isEqualTo("novo-access-token");
        assertThat(response.refreshToken()).isNotEqualTo("token-antigo");
        assertThat(refreshTokenSalvo.isRevogado()).isTrue();

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_deveLancarExcecao_quandoTokenNaoEncontrado() {
        //Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("token-inexistente");

        when(refreshTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessage("Refresh token inválido");
    }

    @Test
    void refresh_deveLancarExcecao_quandoTokenRevogado() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);
        RefreshToken refreshTokenSalvo = new RefreshToken("token-revogado", usuario, Instant.now().plusSeconds(3600));
        refreshTokenSalvo.revogar();

        RefreshTokenRequest request = new RefreshTokenRequest("token-revogado");

        when(refreshTokenRepository.findByToken("token-revogado")).thenReturn(Optional.of(refreshTokenSalvo));

        //Act + Assert
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessage("Refresh Token expirado ou revogado");
    }

    @Test
    void refresh_deveLancarExcecao_quandoTokenExpirado() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);
        RefreshToken refreshTokenSalvo = new RefreshToken("token-expirado", usuario, Instant.now().minusSeconds(1));

        RefreshTokenRequest request = new RefreshTokenRequest("token-expirado");

        when(refreshTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(refreshTokenSalvo));

        //Act + Assert
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(TokenInvalidoException.class)
                .hasMessage("Refresh Token expirado ou revogado");
    }

    @Test
    void logout_deveRevogarToken_quandoTokenExiste() {
        //Arrange
        Usuario usuario = new Usuario("Ana Silva", "ana.silva@email.com", "hash", Role.USER);
        RefreshToken refreshTokenSalvo = new RefreshToken("token-valido", usuario, Instant.now().plusSeconds(3600));

        RefreshTokenRequest request = new RefreshTokenRequest("token-valido");

        when(refreshTokenRepository.findByToken("token-valido")).thenReturn(Optional.of(refreshTokenSalvo));

        //Act
        authService.logout(request);

        //Assert
        assertThat(refreshTokenSalvo.isRevogado()).isTrue();
        verify(refreshTokenRepository).save(refreshTokenSalvo);
    }

    @Test
    void logout_naoDeveFazerNada_quandoTokenNaoExiste() {
        //Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("token-inexistente");

        when(refreshTokenRepository.findByToken("token-inexistente")).thenReturn(Optional.empty());

        //Act
        authService.logout(request);

        //Assert
        verify(refreshTokenRepository, never()).save(any());
    }
}
