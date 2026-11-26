package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As comandas no balcão de ponta a ponta (RF45, ADR 0015): o balcão sem QR, várias comandas com o
 * nome e o cartão, o pedido lançado pela equipe (mesmo com a opção das mesas desligada), a etiqueta na
 * cozinha, na impressão e no painel, a ida para uma mesa, o fechamento e o isolamento.
 */
@IntegrationTest
class CounterTabFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Restaurant(String owner, UUID establishmentId, String waiter, String kitchen, String sector,
                      String table, String counter, String counterToken, String chopp) {
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
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = json(login, "$.accessToken");
        UUID establishment = UUID.fromString(json(login, "$.establishment.id"));
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Bar\"}"), "$.id");
        String table = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.id");
        MvcTestResult counter = send("POST", owner, "/api/v1/tables",
                "{\"label\":\"Balcão do bar\",\"sectorId\":\"%s\",\"kind\":\"COUNTER\"}".formatted(sector));
        assertThat(counter).hasStatus(HttpStatus.CREATED);
        String category = json(send("POST", owner, "/api/v1/menu-categories", "{\"name\":\"Bar\"}"), "$.id");
        String chopp = json(send("POST", owner, "/api/v1/products", """
                {"categoryId":"%s","name":"Chopp","priceCents":1690,"featured":false,"available":true}
                """.formatted(category)), "$.id");
        return new Restaurant(owner, establishment, staff(owner, establishment, "Carlos", Role.STAFF),
                staff(owner, establishment, "Rita", Role.KITCHEN), sector, table, json(counter, "$.id"),
                json(counter, "$.publicToken"), chopp);
    }

    private String staff(String owner, UUID establishment, String name, Role role) throws Exception {
        MvcTestResult added = send("POST", owner, "/api/v1/team-members",
                "{\"name\":\"%s\",\"role\":\"%s\",\"pin\":\"258014\"}".formatted(name, role));
        assertThat(added).hasStatus(HttpStatus.CREATED);
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE id = ?::uuid", UUID.class,
                json(added, "$.id"));
        return tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(establishment), role))
                .value();
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            case "PATCH" -> mvc.patch().uri(uri);
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

    private static List<Object> list(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private MvcTestResult open(String token, String counter, String name, String card) {
        return send("POST", token, "/api/v1/counter-tabs", """
                {"counterId":"%s","name":"%s"%s}""".formatted(counter, name,
                card == null ? "" : ",\"card\":\"" + card + "\""));
    }

    private MvcTestResult order(String token, String tabId, String product) {
        return send("POST", token, "/api/v1/counter-tabs/" + tabId + "/orders", """
                {"clientRequestId":"%s","round":false,"items":[{"productId":"%s","quantity":2}]}
                """.formatted(UUID.randomUUID(), product));
    }

    @Test
    void theCounterIsAPlaceWithoutQrCodeOutsideTheTableLimit() throws Exception {
        MvcTestResult tables = send("GET", sakura.owner(), "/api/v1/tables", null);
        assertThat(list(tables, "$.content[?(@.label == 'Balcão do bar')].kind")).containsExactly("COUNTER");
        assertThat(list(tables, "$.content[?(@.label == '04')].kind")).containsExactly("TABLE");

        // Sem página pública, sem QR.
        assertThat(send("GET", null, "/api/v1/public/tables/" + sakura.counterToken(), null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", sakura.owner(), "/api/v1/tables/" + sakura.counter() + "/token-rotation", null))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        // Fora do limite de mesas do plano: só a mesa 04 conta.
        Long counted = jdbc.queryForObject("SELECT count(*) FROM dining_table WHERE establishment_id = ? AND active "
                + "AND kind = 'TABLE'", Long.class, sakura.establishmentId());
        assertThat(counted).isEqualTo(1L);
        // O nome do balcão pode ser mais comprido que o rótulo da mesa (até 10), mas não passa de 30.
        assertThat(send("POST", sakura.owner(), "/api/v1/tables",
                "{\"label\":\"%s\",\"sectorId\":\"%s\",\"kind\":\"COUNTER\"}".formatted("B".repeat(31),
                        sakura.sector()))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("POST", sakura.owner(), "/api/v1/tables",
                "{\"label\":\"Mesa da janela\",\"sectorId\":\"%s\"}".formatted(sakura.sector())))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void severalTabsAtTheCounterWithNameAndCard() throws Exception {
        MvcTestResult joao = open(sakura.waiter(), sakura.counter(), "João", "12");
        assertThat(joao).hasStatus(HttpStatus.CREATED);
        assertThat(json(joao, "$.atCounter")).isEqualTo("true");
        assertThat(json(joao, "$.customerName")).isEqualTo("João");
        assertThat(json(joao, "$.cardNumber")).isEqualTo("12");
        assertThat(json(joao, "$.tableLabel")).isEqualTo("Balcão do bar");
        MvcTestResult azul = open(sakura.owner(), sakura.counter(), "  camisa   azul ", null);
        assertThat(azul).hasStatus(HttpStatus.CREATED);
        assertThat(json(azul, "$.customerName")).isEqualTo("camisa azul");

        // O mesmo cartão não fica em duas comandas abertas; sem nome, não abre; a cozinha não abre.
        assertThat(open(sakura.owner(), sakura.counter(), "Ana", "12")).hasStatus(HttpStatus.CONFLICT);
        assertThat(open(sakura.owner(), sakura.counter(), " ", null)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(open(sakura.kitchen(), sakura.counter(), "Ana", null)).hasStatus(HttpStatus.FORBIDDEN);
        // Uma mesa não é balcão.
        assertThat(open(sakura.owner(), sakura.table(), "Ana", null)).hasStatus(HttpStatus.NOT_FOUND);

        MvcTestResult listed = send("GET", sakura.waiter(), "/api/v1/counter-tabs?counterId=" + sakura.counter(), null);
        assertThat(list(listed, "$.content[*].customerName")).containsExactly("João", "camisa azul");

        // Corrigir o nome e o cartão; o cartão de outra comanda aberta, não.
        String azulId = json(azul, "$.id");
        MvcTestResult renamed = send("PATCH", sakura.waiter(), "/api/v1/counter-tabs/" + azulId,
                "{\"name\":\"Camisa verde\",\"card\":\"07\"}");
        assertThat(renamed).hasStatusOk();
        assertThat(json(renamed, "$.customerName")).isEqualTo("Camisa verde");
        assertThat(json(renamed, "$.cardNumber")).isEqualTo("07");
        assertThat(send("PATCH", sakura.waiter(), "/api/v1/counter-tabs/" + azulId,
                "{\"name\":\"Camisa verde\",\"card\":\"12\"}")).hasStatus(HttpStatus.CONFLICT);

        // "Comandas abertas" mostra cada comanda do balcão pelo nome.
        MvcTestResult open = send("GET", sakura.owner(), "/api/v1/tabs/open", null);
        assertThat(list(open, "$.content[?(@.atCounter == true)].customerName"))
                .containsExactlyInAnyOrder("João", "Camisa verde");
    }

    @Test
    void theTeamAlwaysOrdersAtTheCounterAndTheKitchenSeesTheName() throws Exception {
        // A opção "garçom pode lançar pedidos" começa desligada: nas mesas não lança, no balcão sim.
        assertThat(json(send("GET", sakura.owner(), "/api/v1/order-settings", null), "$.staffOrderingEnabled"))
                .isEqualTo("false");
        String joao = json(open(sakura.waiter(), sakura.counter(), "João", "12"), "$.id");
        String azul = json(open(sakura.waiter(), sakura.counter(), "Camisa azul", null), "$.id");

        MvcTestResult sheet = send("GET", sakura.waiter(), "/api/v1/counter-tabs/" + joao + "/staff-order", null);
        assertThat(json(sheet, "$.orderingEnabled")).isEqualTo("true");
        assertThat(order(sakura.waiter(), joao, sakura.chopp())).hasStatus(HttpStatus.CREATED);
        assertThat(order(sakura.waiter(), azul, sakura.chopp())).hasStatus(HttpStatus.CREATED);
        // A trava de duplicados vale no balcão: o mesmo chopp de novo, sem confirmar.
        assertThat(order(sakura.waiter(), joao, sakura.chopp())).hasStatus(HttpStatus.CONFLICT);
        // Pelo lugar (a rota da mesa), o balcão não recebe pedido: é preciso escolher a comanda.
        assertThat(send("POST", sakura.waiter(), "/api/v1/tables/" + sakura.counter() + "/orders", """
                {"clientRequestId":"%s","round":false,"items":[{"productId":"%s","quantity":1}]}
                """.formatted(UUID.randomUUID(), sakura.chopp()))).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

        MvcTestResult queue = send("GET", sakura.kitchen(), "/api/v1/order-items", null);
        assertThat(list(queue, "$.content[*].counterName")).containsExactlyInAnyOrder("João", "Camisa azul");
        assertThat(list(queue, "$.content[?(@.counterName == 'João')].counterCard")).containsExactly("12");
        assertThat(list(queue, "$.content[*].tableLabel")).containsOnly("Balcão do bar");
        MvcTestResult print = send("GET", sakura.kitchen(), "/api/v1/print-jobs?kind=ORDER", null);
        assertThat(list(print, "$.content[*].counterName")).containsExactlyInAnyOrder("João", "Camisa azul");

        // O mapa do salão: o balcão é um cartão com as duas comandas e a soma delas; não é mesa ocupada.
        MvcTestResult floor = send("GET", sakura.owner(), "/api/v1/live/floor", null);
        assertThat(list(floor, "$.tables[?(@.kind == 'COUNTER')].counterTabs")).containsExactly(2);
        assertThat(list(floor, "$.tables[?(@.kind == 'COUNTER')].tabTotalCents")).containsExactly(6760);
        assertThat(list(floor, "$.tables[?(@.kind == 'COUNTER')].state")).containsExactly("OCCUPIED");
        assertThat(json(floor, "$.occupied")).isEqualTo("0");
    }

    @Test
    void fromTheCounterToATableAndThenClosed() throws Exception {
        String joao = json(open(sakura.waiter(), sakura.counter(), "João", "12"), "$.id");
        assertThat(order(sakura.waiter(), joao, sakura.chopp())).hasStatus(HttpStatus.CREATED);

        // Para o balcão, não: o destino é sempre uma mesa.
        String other = json(open(sakura.waiter(), sakura.counter(), "Ana", null), "$.id");
        assertThat(send("POST", sakura.waiter(), "/api/v1/tabs/" + joao + "/move",
                "{\"tableId\":\"%s\"}".formatted(sakura.counter()))).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        // O João sentou na mesa 04 (livre): a comanda deixa de ser do balcão, e o nome continua.
        assertThat(send("POST", sakura.waiter(), "/api/v1/tabs/" + joao + "/move",
                "{\"tableId\":\"%s\"}".formatted(sakura.table()))).hasStatusOk();
        MvcTestResult tab = send("GET", sakura.waiter(), "/api/v1/tabs/" + joao, null);
        assertThat(json(tab, "$.atCounter")).isEqualTo("false");
        assertThat(json(tab, "$.customerName")).isEqualTo("João");
        assertThat(json(tab, "$.tableLabel")).isEqualTo("04");
        assertThat(list(send("GET", sakura.waiter(), "/api/v1/counter-tabs?counterId=" + sakura.counter(), null),
                "$.content[*].id")).containsExactly(other);
        // A mesa 04 agora tem uma comanda: "uma por mesa" continua valendo nela.
        assertThat(json(send("GET", sakura.owner(), "/api/v1/live/floor", null), "$.occupied")).isEqualTo("1");

        // Entregar e fechar: o histórico guarda o nome.
        for (Object item : list(send("GET", sakura.kitchen(), "/api/v1/order-items", null), "$.content[*].id")) {
            assertThat(send("POST", sakura.waiter(), "/api/v1/order-items/" + item + "/deliver", null))
                    .hasStatus(HttpStatus.NO_CONTENT);
        }
        assertThat(send("POST", sakura.waiter(), "/api/v1/tabs/" + joao + "/close",
                "{\"paymentMethod\":\"PIX\"}")).hasStatusOk();
        LocalDate today = LocalDate.now();
        MvcTestResult history = send("GET", sakura.owner(), "/api/v1/tabs?from=%s&to=%s".formatted(
                today.minusDays(1), today.plusDays(1)), null);
        assertThat(list(history, "$.content[*].customerName")).containsExactly("João");
    }

    /** Acesso entre restaurantes: o balcão e as comandas do Sakura não existem para outro restaurante. */
    @Test
    void anotherRestaurantGets404() throws Exception {
        String joao = json(open(sakura.waiter(), sakura.counter(), "João", "12"), "$.id");
        Restaurant other = newRestaurant("Cantina");

        assertThat(open(other.owner(), sakura.counter(), "Intruso", null)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.owner(), "/api/v1/counter-tabs?counterId=" + sakura.counter(), null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("PATCH", other.owner(), "/api/v1/counter-tabs/" + joao, "{\"name\":\"Intruso\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("GET", other.owner(), "/api/v1/counter-tabs/" + joao + "/staff-order", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(order(other.owner(), joao, other.chopp())).hasStatus(HttpStatus.NOT_FOUND);
        // O mesmo cartão em outro restaurante é outro cartão.
        assertThat(open(other.owner(), other.counter(), "João", "12")).hasStatus(HttpStatus.CREATED);
        assertThat(list(send("GET", other.owner(), "/api/v1/tabs/open", null), "$.content[*].customerName"))
                .containsExactly("João");
    }
}
