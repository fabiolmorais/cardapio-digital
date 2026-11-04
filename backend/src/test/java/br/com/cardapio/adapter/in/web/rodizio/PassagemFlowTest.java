package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O rodízio de passagem de ponta a ponta: a ativação, o cartão do celular e a tela dos passadores. */
@IntegrationTest
class PassagemFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** O dono, a cozinha (pelo token), a mesa 04 e o Churrasco (passagem). */
    record Restaurant(String owner, String kitchen, String tableId, String qr04, String churrasco) {
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant();
    }

    private Restaurant newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"Fogo de Chão","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(email, PASSWORD));
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = json(login, "$.accessToken");
        String kitchen = tokenIssuer.issue(new TokenSubject(new UserId(UUID.fromString(json(login, "$.userId"))),
                new EstablishmentId(UUID.fromString(json(login, "$.establishment.id"))), Role.KITCHEN)).value();
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        MvcTestResult table = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector));
        String churrasco = json(send("POST", owner, "/api/v1/rodizios", """
                {"name":"Churrasco","type":"PASSAGEM","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":15990,"prices":[]},
                          {"name":"Criança","defaultPriceCents":7990,"prices":[]}],"productIds":[]}
                """), "$.id");
        return new Restaurant(owner, kitchen, json(table, "$.id"), json(table, "$.publicToken"), churrasco);
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            default -> mvc.get().uri(uri);
        };
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), path));
    }

    /** Um celular na mesa, com a presença confirmada pela rede do salão. */
    private Cookie phone(Restaurant r) {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + r.qr04() + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult turn(Restaurant r, Cookie phone, String status) {
        var request = mvc.put().uri("/api/v1/public/tables/" + r.qr04() + "/rodizio/card")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"%s\"}".formatted(status));
        return (phone == null ? request : request.cookie(phone)).exchange();
    }

    private void activate(Restaurant r) {
        assertThat(send("POST", r.owner(), "/api/v1/tables/" + r.tableId() + "/rodizio", """
                {"rodizioId":"%s","guests":[{"tierName":"Adulto","quantity":2},{"tierName":"Criança","quantity":1}]}
                """.formatted(r.churrasco()))).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void thePhoneTurnsTheCardAndThePassersSeeIt() throws Exception {
        Cookie phone = phone(sakura);
        activate(sakura);

        MvcTestResult serving = send("GET", sakura.kitchen(), "/api/v1/passagem-tables", null);
        assertThat(serving).hasStatusOk();
        assertThat(serving).bodyJson().extractingPath("$.content[0].tableLabel").isEqualTo("04");
        assertThat(serving).bodyJson().extractingPath("$.content[0].people").isEqualTo(3);
        assertThat(serving).bodyJson().extractingPath("$.content[0].status").isEqualTo("SERVING");

        MvcTestResult paused = turn(sakura, phone, "PAUSED");
        assertThat(paused).hasStatusOk();
        assertThat(paused).bodyJson().extractingPath("$.status").isEqualTo("PAUSED");
        assertThat(paused).bodyJson().extractingPath("$.type").isEqualTo("PASSAGEM");
        assertThat(send("GET", sakura.kitchen(), "/api/v1/passagem-tables", null)).bodyJson()
                .extractingPath("$.content[0].status").isEqualTo("PAUSED");
        // O celular também vê a cor nos pedidos da mesa.
        assertThat(mvc.get().uri("/api/v1/public/tables/" + sakura.qr04() + "/orders").cookie(phone).exchange())
                .bodyJson().extractingPath("$.rodizio.status").isEqualTo("PAUSED");

        // Fechada a conta, a mesa sai da tela dos passadores.
        String tabId = json(send("GET", sakura.owner(), "/api/v1/tabs/open", null), "$.content[0].id");
        assertThat(send("POST", sakura.owner(), "/api/v1/tabs/" + tabId + "/close", "{\"paymentMethod\":\"PIX\"}"))
                .hasStatusOk();
        assertThat(send("GET", sakura.kitchen(), "/api/v1/passagem-tables", null)).bodyJson()
                .extractingPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void withoutSessionOrRodizioOrLoginNothingTurns() throws Exception {
        assertThat(turn(sakura, null, "PAUSED")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(turn(sakura, phone(sakura), "PAUSED")).hasStatus(HttpStatus.CONFLICT);
        assertThat(turn(sakura, phone(sakura), "VERDE")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("GET", null, "/api/v1/passagem-tables", null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anotherRestaurantSeesNoneOfTheTables() throws Exception {
        activate(sakura);
        Restaurant other = newRestaurant();

        assertThat(send("GET", other.owner(), "/api/v1/passagem-tables", null)).bodyJson()
                .extractingPath("$.totalElements").isEqualTo(0);
        assertThat(send("GET", sakura.owner(), "/api/v1/passagem-tables", null)).bodyJson()
                .extractingPath("$.totalElements").isEqualTo(1);
    }
}
