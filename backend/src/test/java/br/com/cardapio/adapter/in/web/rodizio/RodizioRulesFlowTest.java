package br.com.cardapio.adapter.in.web.rodizio;

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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** As regras do rodízio valendo pela API, e o produto "só do rodízio" fora do carrinho. */
@IntegrationTest
class RodizioRulesFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    private String owner;
    private String tableId;
    private String qr04;
    private String guioza;
    private String sashimi;

    @BeforeEach
    void sakura() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"Sakura Sushi","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(email, PASSWORD));
        owner = json(send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)), "$.accessToken");
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        MvcTestResult table = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector));
        tableId = json(table, "$.id");
        qr04 = json(table, "$.publicToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Japonês\"}"), "$.id");
        guioza = json(send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Guioza","priceCents":2990,"featured":false,"available":true}
                """.formatted(category)), "$.id");
        MvcTestResult onlyRodizio = send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Sashimi","priceCents":4990,"featured":false,"available":true,
                 "aLaCarte":false}
                """.formatted(category));
        assertThat(onlyRodizio).bodyJson().extractingPath("$.aLaCarte").isEqualTo(false);
        sashimi = json(onlyRodizio, "$.id");
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
    private Cookie phone() {
        Cookie cookie = mvc.post().uri("/api/v1/public/tables/" + qr04 + "/session").exchange().getResponse()
                .getCookie("table_session");
        mvc.post().uri("/api/v1/public/tables/" + qr04 + "/presence").cookie(cookie).exchange();
        return cookie;
    }

    private MvcTestResult fromPhone(Cookie phone, String path, String productId, int quantity) {
        return mvc.post().uri("/api/v1/public/tables/" + qr04 + path).cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":\"%s\",\"quantity\":%d}]}".formatted(productId, quantity))
                .exchange();
    }

    @Test
    void theProductOnlyInTheRodizioStaysOutOfTheCart() throws Exception {
        MvcTestResult menu = mvc.get().uri("/api/v1/public/tables/" + qr04 + "/menu").exchange();
        assertThat(menu).bodyJson().extractingPath("$.categories[0].products[?(@.name == 'Sashimi')].aLaCarte")
                .asArray().containsExactly(false);

        MvcTestResult cart = fromPhone(phone(), "/orders", sashimi, 1);
        assertThat(cart).hasStatus(HttpStatus.CONFLICT);
        assertThat(json(cart, "$.detail")).isEqualTo("Sashimi é só do rodízio. Peça pela aba Rodízio.");
    }

    @Test
    void theRulesHoldTheRoundsAndTheTableSeesThem() throws Exception {
        String rodizio = json(send("POST", owner, "/api/v1/rodizios", """
                {"name":"Rodízio Japonês","type":"A_LA_MINUTE",
                 "rules":{"maxPerItemPerRound":1,"maxPendingPerGuest":2,"timeLimitMinutes":60,
                          "wasteNotice":"Sobras são cobradas à parte."},
                 "tiers":[{"name":"Adulto","defaultPriceCents":12990,"prices":[]}],
                 "productIds":["%s","%s"]}
                """.formatted(guioza, sashimi)), "$.id");
        Cookie phone = phone(); // na mesa antes da ativação: entra no rodízio junto
        MvcTestResult activated = send("POST", owner, "/api/v1/tables/" + tableId + "/rodizio",
                "{\"rodizioId\":\"%s\",\"guests\":[{\"tierName\":\"Adulto\",\"quantity\":1}]}".formatted(rodizio));
        assertThat(activated).hasStatus(HttpStatus.CREATED);
        Instant startedAt = Instant.parse(json(activated, "$.startedAt"));
        assertThat(Instant.parse(json(activated, "$.endsAt"))).isEqualTo(startedAt.plus(Duration.ofMinutes(60)));

        MvcTestResult twoSashimis = fromPhone(phone, "/rounds", sashimi, 2);
        assertThat(twoSashimis).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(twoSashimis, "$.detail")).isEqualTo("Nesta rodada, até 1 de Sashimi (1 por pessoa).");
        MvcTestResult first = fromPhone(phone, "/rounds", sashimi, 1);
        assertThat(first).hasStatus(HttpStatus.CREATED);
        assertThat(fromPhone(phone, "/rounds", guioza, 1)).hasStatus(HttpStatus.CREATED);
        // 2 itens não entregues por pessoa: a terceira rodada espera.
        MvcTestResult held = fromPhone(phone, "/rounds", guioza, 1);
        assertThat(held).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(held, "$.detail")).startsWith("Ainda há 2 itens a caminho.");

        MvcTestResult orders = mvc.get().uri("/api/v1/public/tables/" + qr04 + "/orders").cookie(phone).exchange();
        assertThat(orders).bodyJson().extractingPath("$.rodizioLimits.maxPerItem").isEqualTo(1);
        assertThat(orders).bodyJson().extractingPath("$.rodizioLimits.room").isEqualTo(0);
        assertThat(orders).bodyJson().extractingPath("$.rodizioLimits.wasteNotice")
                .isEqualTo("Sobras são cobradas à parte.");
        assertThat(orders).bodyJson().extractingPath("$.rodizio.endsAt").isNotNull();

        // O garçom entrega o sashimi: cabe mais um.
        send("POST", owner, "/api/v1/order-items/" + json(first, "$.items[0].id") + "/deliver", null);
        assertThat(fromPhone(phone, "/rounds", guioza, 1)).hasStatus(HttpStatus.CREATED);
    }
}
