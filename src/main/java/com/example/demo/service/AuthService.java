package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.entity.RefreshToken;
import com.example.demo.entity.Usuario;
import com.example.demo.exception.TokenInvalidoException;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.security.JwtService;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final long expiracaoRefreshTokenMs;

    public AuthService(
            AuthenticationManager authenticationManager,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${jwt.expiracao-refresh-ms}") long expiracaoRefreshTokenMs
    )
    {
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.expiracaoRefreshTokenMs = expiracaoRefreshTokenMs;
    }


    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        Usuario usuario = (Usuario) authentication.getPrincipal();

        String accessToken = jwtService.gerarAccessToken(usuario);
        String refreshToken = gerarESalvarRefreshToken(usuario);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshTokenSalvo = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new TokenInvalidoException("Refresh token inválido"));

        if(!refreshTokenSalvo.isValido()) {
            throw new TokenInvalidoException("Refresh Token expirado ou revogado");
        }

        Usuario usuario = refreshTokenSalvo.getUsuario();

        refreshTokenSalvo.revogar();
        refreshTokenRepository.save(refreshTokenSalvo);

        String novoAccessToken = jwtService.gerarAccessToken(usuario);
        String novoRefreshToken = gerarESalvarRefreshToken(usuario);

        return new TokenResponse(novoAccessToken, novoRefreshToken);

    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> {
                    token.revogar();
                    refreshTokenRepository.save(token);
                });
    }

    private String gerarESalvarRefreshToken(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        Instant expiraEm = Instant.now().plusMillis(expiracaoRefreshTokenMs);

        RefreshToken refreshToken = new RefreshToken(token, usuario, expiraEm);
        refreshTokenRepository.save(refreshToken);

        return token;
    }
}
