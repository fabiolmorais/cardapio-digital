package br.com.cardapio.adapter.in.web.live;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** O salão ao vivo (desenhos 7.1 e 7.2): o estado de cada mesa e o resumo, só do restaurante de quem pede. */
@IntegrationTest
class LiveFloorTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String FLOOR = "/api/v1/live/floor";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Restaurant(String owner, String waiter, String qr04) {
    }

    private Restaurant sakura;

    @BeforeEach
    void sakura() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send(post("/api/v1/signup"), null, """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        String login = send(post("/api/v1/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = JsonPath.read(login, "$.accessToken");
        UUID establishment = UUID.fromString(JsonPath.read(login, "$.establishment.id"));
        send(put("/api/v1/presence-rule"), owner, "{\"networks\":[\"127.0.0.1\"]}");
        String salao = JsonPath.read(send(post("/api/v1/sectors"), owner, "{\"name\":\"Salão principal\"}"), "$.id");
        String deck = JsonPath.read(send(post("/api/v1/sectors"), owner, "{\"name\":\"Deck\"}"), "$.id");
        String qr04 = JsonPath.read(send(post("/api/v1/tables"), owner,
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(salao)), "$.publicToken");
        send(post("/api/v1/tables"), owner, "{\"label\":\"15\",\"sectorId\":\"%s\"}".formatted(deck));
        String added = send(post("/api/v1/team-members"), owner,
                "{\"name\":\"Carlos\",\"role\":\"STAFF\",\"pin\":\"258014\"}");
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE id = ?::uuid", UUID.class,
                (Object) JsonPath.read(added, "$.id"));
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(establishment),
                Role.STAFF)).value();
        return new Restaurant(owner, waiter, qr04);
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

    private void callTheWaiterFrom04(Restaurant r) throws Exception {
        Cookie phone = mvc.perform(post("/api/v1/public/tables/" + r.qr04() + "/session")).andReturn()
                .getResponse().getCookie("table_session");
        mvc.perform(post("/api/v1/public/tables/" + r.qr04() + "/presence").cookie(phone));
        mvc.perform(post("/api/v1/public/tables/" + r.qr04() + "/calls").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"WAITER\"}"));
    }

    @Test
    void theFloorShowsEachTableBySectorAndWhoIsCalling() throws Exception {
        assertThat((List<String>) JsonPath.read(send(get(FLOOR), sakura.owner(), null), "$.tables[*].state"))
                .containsExactly("FREE", "FREE");

        callTheWaiterFrom04(sakura);
        String floor = send(get(FLOOR), sakura.owner(), null);

        assertThat((List<String>) JsonPath.read(floor, "$.tables[*].label")).containsExactly("04", "15");
        assertThat((List<String>) JsonPath.read(floor, "$.tables[*].sectorName"))
                .containsExactly("Salão principal", "Deck");
        assertThat((String) JsonPath.read(floor, "$.tables[0].state")).isEqualTo("CALLING");
        assertThat((String) JsonPath.read(floor, "$.tables[0].call.type")).isEqualTo("WAITER");
        assertThat((Integer) JsonPath.read(floor, "$.tables[0].phones")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(floor, "$.occupied")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(floor, "$.revenueTodayCents")).isZero();
    }

    @Test
    void aCallSomeoneAcceptedLeavesTheTableOccupied() throws Exception {
        callTheWaiterFrom04(sakura);
        String callId = JsonPath.read(send(get("/api/v1/calls"), sakura.waiter(), null), "$.content[0].id");

        send(post("/api/v1/calls/" + callId + "/accept"), sakura.waiter(), null);
        String floor = send(get(FLOOR), sakura.owner(), null);

        assertThat((String) JsonPath.read(floor, "$.tables[0].state")).isEqualTo("OCCUPIED");
        assertThat((String) JsonPath.read(floor, "$.tables[0].call.status")).isEqualTo("IN_PROGRESS");
        assertThat((String) JsonPath.read(floor, "$.tables[0].call.acceptedByName")).isEqualTo("Carlos");
        assertThat((Integer) JsonPath.read(floor, "$.averageWaitSeconds")).isNotNull();
    }

    @Test
    void onlyTheOwnerAndTheManagerSeeTheRevenue() throws Exception {
        String floor = send(get(FLOOR), sakura.waiter(), null);

        assertThat((Object) JsonPath.read(floor, "$.revenueTodayCents")).isNull();
        assertThat((List<?>) JsonPath.read(floor, "$.tables")).hasSize(2);
    }

    /** Separação entre restaurantes: o mapa de um não mostra as mesas nem os chamados do outro. */
    @Test
    void anotherRestaurantDoesNotSeeTheseTables() throws Exception {
        callTheWaiterFrom04(sakura);
        Restaurant other = newRestaurant("Outro Bar");

        String theirs = send(get(FLOOR), other.owner(), null);

        assertThat((List<String>) JsonPath.read(theirs, "$.tables[*].state")).containsExactly("FREE", "FREE");
        assertThat((Integer) JsonPath.read(theirs, "$.occupied")).isZero();
        assertThat(mvc.perform(get(FLOOR)).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
}
