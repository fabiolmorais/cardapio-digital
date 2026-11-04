package br.com.cardapio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
class SecurityConfig {

    /**
     * Documentação da API (Swagger UI). A página do Swagger precisa de estilos inline e imagens
     * embutidas, então recebe um CSP próprio, mais flexível, só nestas rotas.
     * Em produção a documentação é desligada (OPENAPI_ENABLED=false).
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiDocsSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                                        + "frame-ancestors 'none'; object-src 'none'; base-uri 'none'"))
                        .frameOptions(f -> f.deny()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sem sessão; autenticação por token (etapa 2)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Token JWT no cabeçalho Authorization: Bearer. Sem token válido, 401.
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'none'"))
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/plans").permitAll()
                        // Página da mesa (QR Code): o cliente não tem login; quem identifica a mesa é o token.
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        // A sessão da mesa (Guia 12): o cliente não tem login. Só esta rota, e não todo POST público.
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/tables/*/session").permitAll()
                        // Os botões da mesa (Guia 13): também sem login; quem identifica o celular é o cookie da sessão.
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/tables/*/calls").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/public/tables/*/calls/*").permitAll()
                        // A trava de presença (Guia 16): a página da mesa confere se o celular está no salão.
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/tables/*/presence").permitAll()
                        // O pedido pelo celular (Guia 18): enviar o carrinho e cancelar um item na fila.
                        .requestMatchers(HttpMethod.POST, "/api/v1/public/tables/*/orders").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/public/tables/*/order-items/*").permitAll()
                        // O vídeo do prato (Guia 22): o aviso do serviço de vídeo (vale só com o token
                        // secreto, conferido no controller) e o serviço de vídeo falso do desenvolvimento
                        // (as rotas só existem com app.video.provider=fake; o envio vale com a assinatura).
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/video").permitAll()
                        .requestMatchers("/api/v1/dev/fake-video/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        // O aparelho novo ainda não é de ninguém: quem o identifica é o segredo do pedido.
                        .requestMatchers(HttpMethod.POST, "/api/v1/device-pairings",
                                "/api/v1/device-pairings/*/claim").permitAll()
                        // Tela e login por PIN: quem identifica o aparelho é o cookie device_token.
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/device", "/api/v1/auth/device/team-members").permitAll()
                        // Modo TV na TV vinculada (Guia 15): só leitura, e quem identifica a TV é o mesmo cookie.
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/device/tv/calls", "/api/v1/auth/device/tv/events")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/pin").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /** O papel do token (claim "role") vira a autoridade ROLE_OWNER, ROLE_STAFF… usada no @PreAuthorize. */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Os cabeçalhos do envio de vídeo pelo protocolo TUS (Guia 22), para o serviço de vídeo
        // falso, que roda no próprio back. O Bunny de verdade tem o CORS dele.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "Tus-Resumable",
                "Upload-Length", "Upload-Offset", "Upload-Metadata", "AuthorizationSignature", "AuthorizationExpire",
                "VideoId", "LibraryId"));
        // Content-Disposition: o front lê o nome do arquivo baixado (display da mesa). Location e
        // Upload-Offset: o envio TUS do vídeo. Sem estar aqui, o navegador esconde o cabeçalho do
        // JavaScript em respostas de outra origem.
        config.setExposedHeaders(List.of("X-Request-Id", "Content-Disposition", "Location", "Upload-Offset",
                "Tus-Resumable"));
        config.setAllowCredentials(true);
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
