package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration: esta classe é fonte de @Bean para o contexto do Spring.
// @EnableWebSecurity: ativa a configuração de seguranca web e desliga o login/form padrao do Spring Security.
// @EnableMethodSecurity: habilita @PreAuthorize/@PostAuthorize em metodos (usado mais adiante para restringir por Role).
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Filtro que le o header "Authorization: Bearer <token>" e autentica a requisicao via JWT (JwtAuthenticationFilter).
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Implementado por Usuario via CustomUserDetailsService: sabe buscar um usuario pelo email (username).
    private final UserDetailsService userDetailsService;

    // Injecao via construtor, mesmo padrao usado em ContaService.
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    // Bean responsavel por gerar e comparar hashes de senha.
    // BCrypt é lento de proposito e usa salt embutido no hash -> protege contra brute-force e rainbow tables.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider "padrão": sabe autenticar usuario/senha usando UserDetailsService + PasswordEncoder.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // AuthenticationManager: ponto de entrada para autenticar (email, senha) -> usado pelo AuthService no login.
    // O Spring monta esse manager automaticamente usando o(s) AuthenticationProvider(s) registrados como bean acima.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Define as regras de seguranca HTTP: o que é publico, o que exige token, e onde nosso filtro JWT entra.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protege contra ataques que abusam de cookies de sessao automaticos do navegador.
                // A API é stateless (sem cookies de sessão) -> o vetor clássico de CSRF não se aplica, por isso desabilitamos.
                .csrf(csrf -> csrf.disable())

                // Nunca criar/usar HttpSession: cada requisição se autentica do zero, sozinha, via JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Regras avaliadas na ordem declarada; a primeira que casar com a URL "vence".
                .authorizeHttpRequests(auth -> auth
                        // Login/refresh/logout precisam ser acessíveis sem token (ninguém tem token antes de logar).
                        .requestMatchers("/auth/**").permitAll()
                        // Console do H2, liberado só para facilitar o desenvolvimento local.
                        .requestMatchers("/h2-console/**").permitAll()
                        // Catch-all: qualquer outra rota (ex: /contas/**) exige usuário autenticado.
                        .anyRequest().authenticated()
                )

                // Por padrão o Spring envia X-Frame-Options: DENY (anti-clickjacking), o que bloquearia o
                // console do H2 (ele roda dentro de um <iframe>). Desabilitado aqui só por isso.
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // Registra nosso filtro JWT na cadeia, ANTES do filtro padrão de usuario/senha,
                // garantindo que o SecurityContext já esteja populado quando a autorizacao for checada.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
