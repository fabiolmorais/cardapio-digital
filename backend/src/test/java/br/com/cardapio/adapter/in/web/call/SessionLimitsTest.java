package br.com.cardapio.adapter.in.web.call;

import br.com.cardapio.application.port.in.tablesession.EndExpiredSessionsUseCase;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O fim da trava de presença: a validade da sessão escolhida pelo restaurante, o limite de
 * chamados por minuto, o bloqueio de um celular pela equipe e a limpeza das sessões vencidas.
 */
@IntegrationTest
class SessionLimitsTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EndExpiredSessionsUseCase endExpired;

    record Restaurant(String owner, String qr) {

        String table(String path) {
            return "/api/v1/public/tables/" + qr + path;
        }
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        String owner = json(send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)), "$.accessToken");
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        return new Restaurant(owner, qr);
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            case "DELETE" -> mvc.delete().uri(uri);
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

    /** Abre a sessão e confirma a presença pela rede do salão. */
    private Cookie phone(Restaurant restaurant) {
        Cookie cookie = mvc.post().uri(restaurant.table("/session")).exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri(restaurant.table("/presence")).cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult call(Restaurant restaurant, Cookie phone) {
        return mvc.post().uri(restaurant.table("/calls")).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"WAITER\"}").exchange();
    }

    @Test
    void theRestaurantChoosesHowLongTheSessionLasts() throws Exception {
        send("PUT", sakura.owner(), "/api/v1/presence-rule",
                "{\"networks\":[\"127.0.0.1\"],\"sessionIdleMinutes\":30}");

        Cookie cookie = mvc.post().uri(sakura.table("/session")).exchange().getResponse().getCookie("table_session");

        // Meia hora (contada de quando a resposta sai: um ou dois segundos a menos).
        assertThat(cookie.getMaxAge()).isBetween(30 * 60 - 5, 30 * 60);
        // Salvar a tela sem o campo mantém a escolha.
        send("PUT", sakura.owner(), "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        assertThat(json(send("GET", sakura.owner(), "/api/v1/presence-rule", null), "$.sessionIdleMinutes"))
                .isEqualTo("30");
        assertThat(send("PUT", sakura.owner(), "/api/v1/presence-rule", "{\"networks\":[],\"sessionIdleMinutes\":5}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void openingAndCancelingWithoutStopGetsA429() throws Exception {
        Cookie phone = phone(sakura);
        for (int i = 0; i < 5; i++) {
            MvcTestResult created = call(sakura, phone);
            assertThat(created).hasStatus(HttpStatus.CREATED);
            mvc.delete().uri(sakura.table("/calls/" + json(created, "$.id"))).cookie(phone).exchange();
        }

        MvcTestResult sixth = call(sakura, phone);

        assertThat(sixth).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(sixth.getResponse().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    }

    @Test
    void theTeamBlocksAPrankster() throws Exception {
        Cookie prankster = phone(sakura);
        String id = json(call(sakura, prankster), "$.id");

        assertThat(send("POST", sakura.owner(), "/api/v1/calls/" + id + "/block-caller", null))
                .hasStatus(HttpStatus.NO_CONTENT);

        // O chamado dele saiu da fila, ele não chama mais, e a presença diz que está bloqueado.
        assertThat(json(send("GET", sakura.owner(), "/api/v1/calls", null), "$.totalElements")).isEqualTo("0");
        MvcTestResult again = call(sakura, prankster);
        assertThat(again).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(again.getResponse().getContentAsString()).contains("bloqueou");
        MvcTestResult presence = mvc.post().uri(sakura.table("/presence")).cookie(prankster).exchange();
        assertThat(json(presence, "$.blocked")).isEqualTo("true");
        assertThat(json(presence, "$.present")).isEqualTo("false");
        // Os outros celulares da mesa seguem normais.
        assertThat(call(sakura, phone(sakura))).hasStatus(HttpStatus.CREATED);
    }

    /** Separação entre restaurantes: a equipe de outro restaurante não bloqueia ninguém aqui. */
    @Test
    void anotherRestaurantCannotBlock() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        String id = json(call(sakura, phone(sakura)), "$.id");

        assertThat(send("POST", other.owner(), "/api/v1/calls/" + id + "/block-caller", null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void theTeamSeesWhoIsBlockedAndUnblocksThePhone() throws Exception {
        Cookie prankster = phone(sakura);
        String id = json(call(sakura, prankster), "$.id");
        send("POST", sakura.owner(), "/api/v1/calls/" + id + "/block-caller", null);

        MvcTestResult list = send("GET", sakura.owner(), "/api/v1/blocked-phones", null);
        assertThat(list).hasStatusOk();
        assertThat(json(list, "$.totalElements")).isEqualTo("1");
        assertThat(json(list, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(list, "$.content[0].blockedByName")).isNotEqualTo("null");
        String phoneId = json(list, "$.content[0].id");

        assertThat(send("POST", sakura.owner(), "/api/v1/blocked-phones/" + phoneId + "/unblock", null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("POST", sakura.owner(), "/api/v1/blocked-phones/" + phoneId + "/unblock", null))
                .hasStatus(HttpStatus.NO_CONTENT); // de novo: tudo bem

        // A lista esvazia, e o celular volta a chamar.
        assertThat(json(send("GET", sakura.owner(), "/api/v1/blocked-phones", null), "$.totalElements"))
                .isEqualTo("0");
        MvcTestResult presence = mvc.post().uri(sakura.table("/presence")).cookie(prankster).exchange();
        assertThat(json(presence, "$.blocked")).isEqualTo("false");
        assertThat(call(sakura, prankster)).hasStatus(HttpStatus.CREATED);
    }

    /** Separação entre restaurantes: a equipe de outro restaurante não vê nem desbloqueia ninguém daqui. */
    @Test
    void anotherRestaurantNeitherSeesNorUnblocks() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        Cookie prankster = phone(sakura);
        send("POST", sakura.owner(), "/api/v1/calls/" + json(call(sakura, prankster), "$.id") + "/block-caller", null);
        String phoneId = json(send("GET", sakura.owner(), "/api/v1/blocked-phones", null), "$.content[0].id");

        assertThat(json(send("GET", other.owner(), "/api/v1/blocked-phones", null), "$.totalElements"))
                .isEqualTo("0");
        assertThat(send("POST", other.owner(), "/api/v1/blocked-phones/" + phoneId + "/unblock", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(call(sakura, prankster)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void expiredSessionsAreEndedWhenTheyExpired() {
        phone(sakura);
        UUID session = jdbc.queryForObject("""
                SELECT s.id FROM table_session s JOIN dining_table t ON t.id = s.table_id WHERE t.public_token = ?
                """, UUID.class, sakura.qr());
        jdbc.update("UPDATE table_session SET expires_at = now() - interval '1 minute' WHERE id = ?", session);

        assertThat(endExpired.endExpired()).isGreaterThanOrEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT ended_at = expires_at FROM table_session WHERE id = ?",
                Boolean.class, session)).isTrue();
    }
}
