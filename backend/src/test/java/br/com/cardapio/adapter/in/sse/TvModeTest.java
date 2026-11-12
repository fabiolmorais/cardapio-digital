package br.com.cardapio.adapter.in.sse;

import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * O Modo TV na TV vinculada: a fila e o tempo real pelo cookie do aparelho, sem login. Só a TV
 * entra assim, e só no restaurante dela.
 */
@IntegrationTest
class TvModeTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String QUEUE = "/api/v1/auth/device/tv/calls";
    private static final String EVENTS = "/api/v1/auth/device/tv/events";
    private static final String BOARD = "/api/v1/auth/device/tv/board";

    @Autowired
    MockMvc mvc;

    record Restaurant(String owner, String qr) {
    }

    private Restaurant sakura;
    private Cookie tv;

    @BeforeEach
    void sakuraWithATv() throws Exception {
        sakura = newRestaurant("Sakura Sushi");
        tv = link(sakura, "TV");
    }

    private Restaurant newRestaurant(String name) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send(post("/api/v1/signup"), null, """
                {"establishmentName":"%s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        String owner = JsonPath.read(send(post("/api/v1/auth/login"), null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)), "$.accessToken");
        // Trava de presença (Guia 16): o MockMvc chega como 127.0.0.1, então esta é "a rede do salão".
        send(put("/api/v1/presence-rule"), owner, "{\"networks\":[\"127.0.0.1\"]}");
        String sector = JsonPath.read(send(post("/api/v1/sectors"), owner, "{\"name\":\"Salão\"}"), "$.id");
        String qr = JsonPath.read(send(post("/api/v1/tables"), owner,
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        return new Restaurant(owner, qr);
    }

    /** O vínculo do Guia 5: o aparelho pede, o dono vincula com o código, o aparelho busca o cookie. */
    private Cookie link(Restaurant restaurant, String type) throws Exception {
        String started = send(post("/api/v1/device-pairings"), null, "");
        send(post("/api/v1/devices"), restaurant.owner(), """
                {"code":"%s","name":"Aparelho %s","type":"%s"}""".formatted(
                JsonPath.read(started, "$.code"), type, type));
        String id = JsonPath.read(started, "$.id");
        return mvc.perform(post("/api/v1/device-pairings/" + id + "/claim").contentType(MediaType.APPLICATION_JSON)
                .content("{\"secret\":\"%s\"}".formatted((String) JsonPath.read(started, "$.secret"))))
                .andReturn().getResponse().getCookie("device_token");
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

    private String call(Restaurant restaurant) throws Exception {
        Cookie phone = mvc.perform(post("/api/v1/public/tables/" + restaurant.qr() + "/session")).andReturn()
                .getResponse().getCookie("table_session");
        mvc.perform(post("/api/v1/public/tables/" + restaurant.qr() + "/presence").cookie(phone));
        return JsonPath.read(mvc.perform(post("/api/v1/public/tables/" + restaurant.qr() + "/calls").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"WAITER\"}")).andReturn()
                .getResponse().getContentAsString(), "$.id");
    }

    private int status(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private static String awaitText(MvcResult stream, String expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            String text = stream.getResponse().getContentAsString();
            if (text.contains(expected)) {
                return text;
            }
            Thread.sleep(50);
        }
        return fail("Não chegou \"%s\" em 5 s. Chegou: %s", expected, stream.getResponse().getContentAsString());
    }

    @Test
    void theTvSeesTheQueueWithoutLogin() throws Exception {
        String id = call(sakura);

        String queue = mvc.perform(get(QUEUE).cookie(tv)).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(queue, "$.content[0].id")).isEqualTo(id);
        assertThat((String) JsonPath.read(queue, "$.content[0].tableLabel")).isEqualTo("04");
    }

    @Test
    void theTvHearsTheNewCall() throws Exception {
        MvcResult stream = mvc.perform(get(EVENTS).cookie(tv).accept(MediaType.TEXT_EVENT_STREAM)).andReturn();
        awaitText(stream, "event:ready");

        String id = call(sakura);

        assertThat(awaitText(stream, id)).contains("event:call", "\"status\":\"PENDING\"");
    }

    @Test
    void onlyALinkedTvEntersThisWay() throws Exception {
        Cookie phone = link(sakura, "WAITER");

        assertThat(status(get(QUEUE))).isEqualTo(401);
        assertThat(status(get(QUEUE).cookie(new Cookie("device_token", "inventado")))).isEqualTo(401);
        assertThat(status(get(QUEUE).cookie(phone))).isEqualTo(403);
        assertThat(status(get(EVENTS).cookie(phone).accept(MediaType.TEXT_EVENT_STREAM))).isEqualTo(403);
    }

    @Test
    void anUnlinkedTvStopsSeeingTheQueue() throws Exception {
        String devices = send(get("/api/v1/devices"), sakura.owner(), null);
        String tvId = JsonPath.read(devices, "$.content[?(@.type == 'TV')].id").toString()
                .replaceAll("[\\[\\]\"]", "");

        send(delete("/api/v1/devices/" + tvId), sakura.owner(), null);

        assertThat(status(get(QUEUE).cookie(tv))).isEqualTo(401);
    }

    /** Um pedido de 2 guiozas na mesa 04, pelo celular. */
    private void order(Restaurant restaurant) throws Exception {
        String category = JsonPath.read(send(post("/api/v1/menu-categories"), restaurant.owner(),
                "{\"name\":\"Entradas\"}"), "$.id");
        String product = JsonPath.read(send(post("/api/v1/products"), restaurant.owner(), """
                {"categoryId":"%s","name":"Guioza","priceCents":2990,"featured":false,"available":true}
                """.formatted(category)), "$.id");
        Cookie phone = mvc.perform(post("/api/v1/public/tables/" + restaurant.qr() + "/session")).andReturn()
                .getResponse().getCookie("table_session");
        mvc.perform(post("/api/v1/public/tables/" + restaurant.qr() + "/presence").cookie(phone));
        mvc.perform(post("/api/v1/public/tables/" + restaurant.qr() + "/orders").cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"%s\",\"quantity\":2}]}".formatted(product)));
    }

    @Test
    void theTvSeesTheKitchenAndTheSectorsWithoutLogin() throws Exception {
        order(sakura);

        String board = mvc.perform(get(BOARD).cookie(tv)).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(board, "$.kitchen[0].productName")).isEqualTo("Guioza");
        assertThat((String) JsonPath.read(board, "$.kitchen[0].tableLabel")).isEqualTo("04");
        assertThat((String) JsonPath.read(board, "$.kitchen[0].sectorName")).isEqualTo("Salão");
        assertThat(board).contains("\"sectors\":[\"Salão\"]");
        assertThat(status(get(BOARD))).isEqualTo(401);
        assertThat(status(get(BOARD).cookie(link(sakura, "WAITER")))).isEqualTo(403);
    }

    @Test
    void theTvOfAnotherRestaurantDoesNotSeeTheKitchen() throws Exception {
        order(sakura);
        Cookie theirTv = link(newRestaurant("Outro Bar"), "TV");

        String theirs = mvc.perform(get(BOARD).cookie(theirTv)).andReturn().getResponse().getContentAsString();

        assertThat((java.util.List<?>) JsonPath.read(theirs, "$.kitchen")).isEmpty();
    }

    /** Separação entre restaurantes: a TV de um não vê a fila nem ouve os avisos do outro. */
    @Test
    void theTvOfAnotherRestaurantSeesNothing() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        Cookie theirTv = link(other, "TV");
        MvcResult theirs = mvc.perform(get(EVENTS).cookie(theirTv).accept(MediaType.TEXT_EVENT_STREAM)).andReturn();
        MvcResult ours = mvc.perform(get(EVENTS).cookie(tv).accept(MediaType.TEXT_EVENT_STREAM)).andReturn();
        awaitText(theirs, "event:ready");
        awaitText(ours, "event:ready");

        String id = call(sakura);
        awaitText(ours, id);
        Thread.sleep(300);

        assertThat(theirs.getResponse().getContentAsString()).doesNotContain("event:call");
        String theirQueue = mvc.perform(get(QUEUE).cookie(theirTv)).andReturn().getResponse().getContentAsString();
        assertThat((Integer) JsonPath.read(theirQueue, "$.totalElements")).isZero();
    }
}
