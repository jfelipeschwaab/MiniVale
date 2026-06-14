package com.example.demo.security;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


//Filtro HTTP que roda em toda requisição que chega na minha API
//1. Olha o header Authorization em busca de um JWT
//2. Se houver um token válido, autentica o usuário para a requisição (sem precisar de sessão/cookie)
//3. Se não houver token (ou for inválido), deixa a requisição seguir sem autenticar
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
            ) throws ServletException, IOException
    {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(PREFIXO_BEARER)) {
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(PREFIXO_BEARER.length());

        try {
            String email = jwtService.extrairEmail(token);

            if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValido(token, usuario)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException ignored) {
            // Token ausente/invalido/expirado: seguimos sem autenticar.
            // O SecurityConfig decide se o endpoint exige autenticacao.
        }

        filterChain.doFilter(request,response);

    }


}
