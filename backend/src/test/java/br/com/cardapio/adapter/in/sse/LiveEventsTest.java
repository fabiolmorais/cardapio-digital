package br.com.cardapio.adapter.in.sse;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * O tempo real de ponta a ponta: a mesa chama pela API, o aviso passa pelo NOTIFY do Postgres e
 * chega às conexões SSE abertas, a da equipe e a do celular daquela mesa, e de mais ninguém.
 *
 * Usa o MockMvc "clássico": a conexão SSE fica aberta (assíncrona), e o que o servidor manda vai
 * aparecendo no corpo da resposta, que o teste lê de tempos em tempos.
 */
@IntegrationTest
class LiveEventsTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvc mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    record Restaurant(String owner, UUID establishmentId, String qr04, String qr05) {
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send(post("/api/v1/signup"), null, """
                {"establishmentName":"%s","ownerName":"Marina Lopes","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        String login = send(post("/api/v1/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = JsonPath.read(login, "$.accessToken");
        UUID establishment = UUID.fromString(JsonPath.read(login, "$.establishment.id"));
        // Trava de presença (Guia 16): o MockMvc chega como 127.0.0.1, então esta é "a rede do salão".
        send(put("/api/v1/presence-rule"), owner, "{\"networks\":[\"127.0.0.1\"]}");
        String sector = JsonPath.read(send(post("/api/v1/sectors"), owner, "{\"name\":\"Salão\"}"), "$.id");
        String qr04 = JsonPath.read(send(post("/api/v1/tables"), owner,
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String qr05 = JsonPath.read(send(post("/api/v1/tables"), owner,
                "{\"label\":\"05\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        return new Restaurant(owner, establishment, qr04, qr05);
    }

    private String send(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mvc.perform(request).andReturn().getResponse().getContentAsString();
    }

    private String staff(Restaurant restaurant, Role role) {
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE establishment_id = ? LIMIT 1",
                UUID.class, restaurant.establishmentId());
        return tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(restaurant.establishmentId()),
                role)).value();
    }

    /** Abre a sessão do celular e confirma a presença (pela rede do salão). */
    private Cookie session(String qr) throws Exception {
        Cookie cookie = mvc.perform(post("/api/v1/public/tables/" + qr + "/session")).andReturn().getResponse()
                .getCookie("table_session");
        mvc.perform(post("/api/v1/public/tables/" + qr + "/presence").cookie(cookie));
        return cookie;
    }

    private String call(String qr, Cookie phone, String body) throws Exception {
        return mvc.perform(post("/api/v1/public/tables/" + qr + "/calls").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getContentAsString();
    }

    /** Abre a conexão SSE e espera o primeiro aviso, o "ready". */
    private MvcResult open(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult stream = mvc.perform(request.accept(MediaType.TEXT_EVENT_STREAM)).andReturn();
        assertThat(stream.getRequest().isAsyncStarted()).isTrue();
        awaitText(stream, "event:ready");
        return stream;
    }

    private MvcResult staffStream(String token) throws Exception {
        return open(get("/api/v1/events").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private MvcResult tableStream(String qr, Cookie phone) throws Exception {
        return open(get("/api/v1/public/tables/" + qr + "/events").cookie(phone));
    }

    /** O que chegou até agora na conexão aberta. */
    private static String text(MvcResult stream) throws Exception {
        return stream.getResponse().getContentAsString();
    }

    private static String awaitText(MvcResult stream, String expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            String text = text(stream);
            if (text.contains(expected)) {
                return text;
            }
            Thread.sleep(50);
        }
        return fail("Não chegou \"%s\" em 5 s. Chegou: %s", expected, text(stream));
    }

    @Test
    void theTeamHearsEachStepOfTheCall() throws Exception {
        MvcResult team = staffStream(staff(sakura, Role.STAFF));
        Cookie phone = session(sakura.qr04());

        String id = JsonPath.read(call(sakura.qr04(), phone, "{\"type\":\"WAITER\"}"), "$.id");
        awaitText(team, "\"status\":\"PENDING\"");

        send(post("/api/v1/calls/" + id + "/accept"), sakura.owner(), null);
        String text = awaitText(team, "\"status\":\"IN_PROGRESS\"");

        assertThat(text).contains("event:call", "\"callId\":\"" + id + "\"", "\"type\":\"WAITER\"");
        // Só ids e status: nenhum nome de pessoa viaja pelo tempo real.
        assertThat(text).doesNotContain("Marina");
    }

    @Test
    void theKitchenAlsoListens() throws Exception {
        MvcResult kitchen = staffStream(staff(sakura, Role.KITCHEN));

        call(sakura.qr04(), session(sakura.qr04()), "{\"type\":\"BILL\",\"paymentMethod\":\"PIX\"}");

        awaitText(kitchen, "\"type\":\"BILL\"");
    }

    @Test
    void thePhoneHearsOnlyItsOwnTable() throws Exception {
        Cookie phone04 = session(sakura.qr04());
        MvcResult mesa04 = tableStream(sakura.qr04(), phone04);
        MvcResult team = staffStream(staff(sakura, Role.STAFF));

        String other = JsonPath.read(call(sakura.qr05(), session(sakura.qr05()), "{\"type\":\"WAITER\"}"), "$.id");
        awaitText(team, other); // a equipe recebeu; a mesa 04, não
        Thread.sleep(300);
        assertThat(text(mesa04)).doesNotContain("event:call");

        String mine = JsonPath.read(call(sakura.qr04(), phone04, "{\"type\":\"WAITER\"}"), "$.id");
        assertThat(awaitText(mesa04, mine)).doesNotContain("tableId");
    }

    /** Separação entre restaurantes: o canal de um nunca recebe nada do outro. */
    @Test
    void anotherRestaurantHearsNothing() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        MvcResult theirs = staffStream(staff(other, Role.OWNER));
        MvcResult ours = staffStream(staff(sakura, Role.OWNER));

        String id = JsonPath.read(call(sakura.qr04(), session(sakura.qr04()), "{\"type\":\"WAITER\"}"), "$.id");
        awaitText(ours, id);
        Thread.sleep(300);

        assertThat(text(theirs)).doesNotContain("event:call");
    }

    @Test
    void withoutLoginOrSessionThereIsNoStream() throws Exception {
        assertThat(mvc.perform(get("/api/v1/events").accept(MediaType.TEXT_EVENT_STREAM)).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(get("/api/v1/public/tables/" + sakura.qr04() + "/events")
                .accept(MediaType.TEXT_EVENT_STREAM)).andReturn().getResponse().getStatus()).isEqualTo(401);
        // O cookie da mesa 05 não abre o canal da 04.
        assertThat(mvc.perform(get("/api/v1/public/tables/" + sakura.qr04() + "/events")
                .cookie(session(sakura.qr05())).accept(MediaType.TEXT_EVENT_STREAM)).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
    }
}
