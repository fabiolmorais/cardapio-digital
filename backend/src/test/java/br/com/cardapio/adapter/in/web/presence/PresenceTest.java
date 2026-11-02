package br.com.cardapio.adapter.in.web.presence;

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

/**
 * A trava de presença de ponta a ponta: o dono configura, o celular prova pela rede ou pela
 * localização, e quem não consegue pede liberação ao garçom.
 *
 * O IP do "celular" é o endereço da conexão (setRemoteAddr). O MockMvc usa 127.0.0.1 se nada for dito.
 */
@IntegrationTest
class PresenceTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String SALAO = "189.45.212.18";
    private static final String QUATRO_G = "177.10.20.30";
    private static final String RULE = "/api/v1/presence-rule";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    record Restaurant(String owner, UUID userId, UUID establishmentId, String qr) {

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
                {"establishmentName":"%s","ownerName":"Marina Lopes","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(name, email, PASSWORD));
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        String owner = json(login, "$.accessToken");
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão\"}"), "$.id");
        String qr = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        return new Restaurant(owner, UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")), qr);
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

    private Cookie phone() {
        return mvc.post().uri(sakura.table("/session")).exchange().getResponse().getCookie("table_session");
    }

    /** O celular pergunta "estou no salão?", de um IP, com ou sem a localização. */
    private MvcTestResult presence(Cookie phone, String ip, String body) {
        var request = mvc.post().uri(sakura.table("/presence")).cookie(phone)
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                });
        return body == null ? request.exchange() : request.contentType(MediaType.APPLICATION_JSON).content(body)
                .exchange();
    }

    private MvcTestResult call(Cookie phone, String type) {
        return mvc.post().uri(sakura.table("/calls")).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"%s\"}".formatted(type)).exchange();
    }

    private void configure(String body) {
        assertThat(send("PUT", sakura.owner(), RULE, body)).hasStatusOk();
    }

    @Test
    void theOwnerConfiguresAndSeesTheNetworkWhereHeIs() throws Exception {
        MvcTestResult saved = send("PUT", sakura.owner(), RULE, """
                {"networks":["189.45.212.18","2804:14c:6a::/48"],"wifiName":"Sakura Clientes",
                 "location":{"latitude":-23.5555,"longitude":-46.6355,"radiusMeters":100}}""");

        assertThat(saved).hasStatusOk();
        assertThat(saved).bodyJson().extractingPath("$.networks").asArray()
                .containsExactly("189.45.212.18/32", "2804:14c:6a:0:0:0:0:0/48");
        assertThat(json(saved, "$.location.radiusMeters")).isEqualTo("100");
        assertThat(json(send("GET", sakura.owner(), RULE, null), "$.currentNetwork")).isEqualTo("127.0.0.1/32");
    }

    @Test
    void wrongValuesAreRefused() {
        assertThat(send("PUT", sakura.owner(), RULE, "{\"networks\":[\"restaurante.com\"]}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(send("PUT", sakura.owner(), RULE, """
                {"networks":[],"location":{"latitude":-23.5,"longitude":-46.6,"radiusMeters":5000}}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void onTheRestaurantWifiThePhoneCallsRightAway() throws Exception {
        configure("{\"networks\":[\"" + SALAO + "\"]}");
        Cookie phone = phone();

        MvcTestResult status = presence(phone, SALAO, null);

        assertThat(json(status, "$.present")).isEqualTo("true");
        assertThat(json(status, "$.method")).isEqualTo("WIFI");
        assertThat(call(phone, "WAITER")).hasStatus(HttpStatus.CREATED);
        MvcTestResult queue = send("GET", sakura.owner(), "/api/v1/calls", null);
        assertThat(json(queue, "$.content[0].presenceMethod")).isEqualTo("WIFI");
    }

    @Test
    void outsideTheWifiTheLocationCanProveIt() throws Exception {
        configure("""
                {"networks":["%s"],"location":{"latitude":-23.5555,"longitude":-46.6355,"radiusMeters":100}}"""
                .formatted(SALAO));
        Cookie phone = phone();

        MvcTestResult first = presence(phone, QUATRO_G, null);
        assertThat(json(first, "$.present")).isEqualTo("false");
        assertThat(json(first, "$.locationEnabled")).isEqualTo("true");
        assertThat(call(phone, "WAITER")).hasStatus(HttpStatus.FORBIDDEN);

        MvcTestResult far = presence(phone, QUATRO_G,
                "{\"latitude\":-23.57,\"longitude\":-46.6355,\"accuracyMeters\":20}");
        assertThat(json(far, "$.present")).isEqualTo("false");

        MvcTestResult near = presence(phone, QUATRO_G,
                "{\"latitude\":-23.5556,\"longitude\":-46.6355,\"accuracyMeters\":20}");
        assertThat(json(near, "$.method")).isEqualTo("GPS");
        assertThat(call(phone, "BILL").getResponse().getStatus()).isEqualTo(400); // a conta pede a forma de pagamento
        assertThat(mvc.post().uri(sakura.table("/calls")).cookie(phone).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BILL\",\"paymentMethod\":\"PIX\"}").exchange()).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void withoutAnyProofTheWaiterUnlocksTheWholeTable() throws Exception {
        Cookie phone = phone();
        Cookie friend = phone();
        assertThat(json(presence(phone, QUATRO_G, null), "$.present")).isEqualTo("false");
        assertThat(call(phone, "WAITER")).hasStatus(HttpStatus.FORBIDDEN);

        MvcTestResult unlock = call(phone, "UNLOCK");
        assertThat(unlock).hasStatus(HttpStatus.CREATED);
        MvcTestResult queue = send("GET", sakura.owner(), "/api/v1/calls", null);
        assertThat(json(queue, "$.content[0].type")).isEqualTo("UNLOCK");
        assertThat(json(queue, "$.content[0].presenceMethod")).isEqualTo("null");

        MvcTestResult released = send("POST", sakura.owner(), "/api/v1/calls/" + json(unlock, "$.id") + "/accept",
                null);
        assertThat(released).hasStatusOk();
        assertThat(json(released, "$.status")).isEqualTo("DONE");

        // Os dois celulares da mesa ficam liberados, e a liberação mostra quem liberou.
        for (Cookie c : new Cookie[]{phone, friend}) {
            assertThat(json(presence(c, QUATRO_G, null), "$.method")).isEqualTo("STAFF");
        }
        assertThat(call(friend, "WAITER")).hasStatus(HttpStatus.CREATED);
        assertThat(call(phone, "UNLOCK")).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void theWaiterCanDeclineAPhoneThatIsNotThere() throws Exception {
        Cookie phone = phone();
        String id = json(call(phone, "UNLOCK"), "$.id");

        MvcTestResult declined = send("POST", sakura.owner(), "/api/v1/calls/" + id + "/decline", null);

        assertThat(declined).hasStatusOk();
        assertThat(json(declined, "$.status")).isEqualTo("CANCELED");
        assertThat(json(presence(phone, QUATRO_G, null), "$.present")).isEqualTo("false");
        assertThat(call(phone, "UNLOCK")).hasStatus(HttpStatus.CREATED); // pode pedir de novo
    }

    @Test
    void onlyTheOwnerOrManagerConfigures() {
        String waiter = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                new EstablishmentId(sakura.establishmentId()), Role.STAFF)).value();

        assertThat(send("PUT", waiter, RULE, "{\"networks\":[]}")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", null, RULE, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Separação entre restaurantes: a configuração de um não aparece nem vale no outro. */
    @Test
    void anotherRestaurantHasItsOwnRule() throws Exception {
        configure("{\"networks\":[\"" + SALAO + "\"]}");
        Restaurant other = newRestaurant("Outro Bar");
        Cookie theirPhone = mvc.post().uri(other.table("/session")).exchange().getResponse()
                .getCookie("table_session");

        assertThat(send("GET", other.owner(), RULE, null)).bodyJson().extractingPath("$.networks").asArray()
                .isEmpty();
        MvcTestResult theirs = mvc.post().uri(other.table("/presence")).cookie(theirPhone)
                .with(r -> {
                    r.setRemoteAddr(SALAO);
                    return r;
                }).exchange();
        assertThat(json(theirs, "$.present")).isEqualTo("false");
        String unlock = json(call(phone(), "UNLOCK"), "$.id");
        assertThat(send("POST", other.owner(), "/api/v1/calls/" + unlock + "/accept", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send("POST", other.owner(), "/api/v1/calls/" + unlock + "/decline", null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
