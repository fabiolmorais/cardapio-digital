package br.com.cardapio.adapter.in.web;

import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O limite por IP ligado, com limites pequenos (nos outros testes ele fica desligado). Cada teste
 * usa o seu próprio IP: o contador é um só para o contexto inteiro.
 */
@IntegrationTest
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.rules.login.requests=3",
        "app.rate-limit.rules.table-actions.requests=2"
})
class RateLimitTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    /** A requisição vinda de outra rede (o MockMvc usa 127.0.0.1). */
    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private MvcTestResult login(String ip, String email) {
        return mvc.post().uri("/api/v1/auth/login")
                .with(from(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"senha-errada-123\"}".formatted(email))
                .exchange();
    }

    @Test
    void theFourthLoginFromTheSameNetworkWaits() throws Exception {
        String email = "ninguem-" + UUID.randomUUID() + "@teste.com";
        for (int i = 0; i < 3; i++) {
            assertThat(login("203.0.113.10", email)).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        MvcTestResult blocked = login("203.0.113.10", email);

        assertThat(blocked).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked).headers().containsHeader(HttpHeaders.RETRY_AFTER);
        assertThat(blocked.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(JsonPath.<String>read(blocked.getResponse().getContentAsString(), "$.detail"))
                .isEqualTo("Muitas tentativas desta rede. Espere 10 minutos e tente de novo.");
        // Outra rede continua tentando (e recebe o 401 de sempre, não o 429).
        assertThat(login("203.0.113.11", email)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theTableActionsCountPerNetworkButReadingDoesNot() throws Exception {
        String qr = "qr-que-nao-existe-" + UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            // A mesa não existe: 404, mas a tentativa conta (é o que um robô faria).
            assertThat(mvc.post().uri("/api/v1/public/tables/" + qr + "/calls")
                    .with(from("203.0.113.20"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"WAITER\"}").exchange())
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        assertThat(mvc.post().uri("/api/v1/public/tables/" + qr + "/orders")
                .with(from("203.0.113.20"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}").exchange())
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        for (int i = 0; i < 5; i++) {
            assertThat(mvc.get().uri("/api/v1/public/tables/" + qr)
                    .with(from("203.0.113.20")).exchange())
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }
}
