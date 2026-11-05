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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O pedido de corte de ponta a ponta: o celular pede, o passador vê e entrega, e a cozinha não recebe. */
@IntegrationTest
class PassagemRequestsFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    private String owner;
    private String qr04;
    private String picanha;
    private String guarana;

    @BeforeEach
    void churrascaria() throws Exception {
        owner = newOwner();
        // O MockMvc chega como 127.0.0.1: esta é "a rede do salão".
        send("PUT", owner, "/api/v1/presence-rule", "{\"networks\":[\"127.0.0.1\"]}");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        MvcTestResult table = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector));
        qr04 = json(table, "$.publicToken");
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Carnes\"}"), "$.id");
        picanha = json(send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Picanha","priceCents":0,"featured":false,"available":true,
                 "aLaCarte":false}
                """.formatted(category)), "$.id");
        guarana = json(send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Guaraná","priceCents":690,"featured":false,"available":true}
                """.formatted(category)), "$.id");
        String churrasco = json(send("POST", owner, "/api/v1/rodizios", """
                {"name":"Churrasco","type":"PASSAGEM","rules":{},
                 "tiers":[{"name":"Adulto","defaultPriceCents":15990,"prices":[]}],"productIds":["%s"]}
                """.formatted(picanha)), "$.id");
        assertThat(send("POST", owner, "/api/v1/tables/" + json(table, "$.id") + "/rodizio",
                "{\"rodizioId\":\"%s\",\"guests\":[{\"tierName\":\"Adulto\",\"quantity\":2}]}".formatted(churrasco)))
                .hasStatus(HttpStatus.CREATED);
    }

    private String newOwner() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        send("POST", null, "/api/v1/signup", """
                {"establishmentName":"Fogo de Chão","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(email, PASSWORD));
        return json(send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)), "$.accessToken");
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

    private MvcTestResult fromPhone(Cookie phone, String path, String productId, String note) {
        return mvc.post().uri("/api/v1/public/tables/" + qr04 + path).cookie(phone)
                .contentType(MediaType.APPLICATION_JSON)
                .content(note == null
                        ? "{\"items\":[{\"productId\":\"%s\",\"quantity\":1}]}".formatted(productId)
                        : "{\"items\":[{\"productId\":\"%s\",\"quantity\":1,\"note\":\"%s\"}]}".formatted(productId,
                        note))
                .exchange();
    }

    @Test
    void theCutGoesToThePassersAndNotToTheKitchen() throws Exception {
        Cookie phone = phone();
        MvcTestResult cut = fromPhone(phone, "/rounds", picanha, "Mal passada");
        assertThat(cut).hasStatus(HttpStatus.CREATED);
        assertThat(cut).bodyJson().extractingPath("$.items[0].unitPriceCents").isEqualTo(0);
        assertThat(fromPhone(phone, "/orders", guarana, null)).hasStatus(HttpStatus.CREATED);

        MvcTestResult passers = send("GET", owner, "/api/v1/passagem-requests", null);
        assertThat(passers).bodyJson().extractingPath("$.content[*].productName").asArray().containsExactly("Picanha");
        assertThat(passers).bodyJson().extractingPath("$.content[0].note").isEqualTo("Mal passada");
        assertThat(passers).bodyJson().extractingPath("$.content[0].tableLabel").isEqualTo("04");
        // A cozinha só vê o guaraná.
        assertThat(send("GET", owner, "/api/v1/order-items", null)).bodyJson()
                .extractingPath("$.content[*].productName").asArray().containsExactly("Guaraná");

        // O passador leva o corte: ele sai da fila, e a mesa vê "Entregue".
        assertThat(send("POST", owner, "/api/v1/order-items/" + json(passers, "$.content[0].id") + "/deliver", null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(send("GET", owner, "/api/v1/passagem-requests", null)).bodyJson()
                .extractingPath("$.totalElements").isEqualTo(0);
        assertThat(mvc.get().uri("/api/v1/public/tables/" + qr04 + "/orders").cookie(phone).exchange()).bodyJson()
                .extractingPath("$.orders[0].items[0].status").isEqualTo("DELIVERED");
    }

    @Test
    void anotherRestaurantSeesNoneOfTheCuts() throws Exception {
        assertThat(fromPhone(phone(), "/rounds", picanha, "Ao ponto")).hasStatus(HttpStatus.CREATED);
        String other = newOwner();

        assertThat(send("GET", other, "/api/v1/passagem-requests", null)).bodyJson()
                .extractingPath("$.totalElements").isEqualTo(0);
        assertThat(send("GET", null, "/api/v1/passagem-requests", null)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(send("GET", owner, "/api/v1/passagem-requests", null)).bodyJson()
                .extractingPath("$.totalElements").isEqualTo(1);
    }
}
