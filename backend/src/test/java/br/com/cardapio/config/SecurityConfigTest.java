package br.com.cardapio.config;

import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SecurityConfigTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void healthCheckIsPublic() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
    }

    @Test
    void unknownEndpointsRequireAuthentication() {
        assertThat(mvc.get().uri("/api/v1/tables")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/api/v1/plans")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sendsSecurityHeaders() {
        var result = mvc.get().uri("/api/v1/plans");
        assertThat(result).headers()
                .hasValue("X-Frame-Options", "DENY")
                .hasValue("X-Content-Type-Options", "nosniff")
                .containsHeader("Content-Security-Policy")
                .containsHeader("X-Request-Id");
    }

    @Test
    void corsAllowsOnlyConfiguredOrigin() {
        var allowed = mvc.options().uri("/api/v1/plans")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET");
        assertThat(allowed).hasStatusOk().headers()
                .hasValue("Access-Control-Allow-Origin", "http://localhost:3000");

        var denied = mvc.options().uri("/api/v1/plans")
                .header("Origin", "https://site-malicioso.example")
                .header("Access-Control-Request-Method", "GET");
        assertThat(denied).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void ignoresUnsafeRequestId() {
        var result = mvc.get().uri("/api/v1/plans").header("X-Request-Id", "<script>alert(1)</script>");
        assertThat(result.exchange().getResponse().getHeader("X-Request-Id"))
                .isNotEqualTo("<script>alert(1)</script>")
                .matches("[A-Za-z0-9-]{8,64}");
    }
}
