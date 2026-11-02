package br.com.cardapio.adapter.in.web.call;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os chamados de ponta a ponta: a mesa chama (com o cookie da sessão), a equipe vê na fila,
 * aceita e conclui. Inclui as corridas (dois toques, dois garçons) e a separação entre restaurantes.
 */
@IntegrationTest
class CallFlowTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    /** Um restaurante com o salão, a mesa 04 e a 05, e um garçom (Carlos) já com token. */
    record Restaurant(String ownerToken, UUID establishmentId, String qr04, String qr05, String carlos) {

        String calls(String qr) {
            return "/api/v1/public/tables/" + qr + "/calls";
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
        UUID establishment = UUID.fromString(json(login, "$.establishment.id"));
        String sector = json(send("POST", owner, "/api/v1/sectors", "{\"name\":\"Salão principal\"}"), "$.id");
        String qr04 = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"04\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String qr05 = json(send("POST", owner, "/api/v1/tables",
                "{\"label\":\"05\",\"sectorId\":\"%s\"}".formatted(sector)), "$.publicToken");
        String carlos = staff(owner, establishment, "Carlos Souza", Role.STAFF);
        return new Restaurant(owner, establishment, qr04, qr05, carlos);
    }

    /** Cadastra a pessoa pela API e emite o token dela direto (o login por PIN precisa de aparelho). */
    private String staff(String owner, UUID establishment, String name, Role role) throws Exception {
        MvcTestResult added = send("POST", owner, "/api/v1/team-members",
                "{\"name\":\"%s\",\"role\":\"%s\",\"pin\":\"258014\"}".formatted(name, role));
        assertThat(added).hasStatus(HttpStatus.CREATED);
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE id = ?::uuid", UUID.class,
                json(added, "$.id"));
        return tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(establishment), role))
                .value();
    }

    private Cookie session(String qr) {
        MvcTestResult started = mvc.post().uri("/api/v1/public/tables/" + qr + "/session").exchange();
        assertThat(started).hasStatusOk();
        return started.getResponse().getCookie("table_session");
    }

    private MvcTestResult call(String qr, Cookie cookie, String body) {
        var request = mvc.post().uri(sakura.calls(qr)).contentType(MediaType.APPLICATION_JSON).content(body);
        return (cookie == null ? request : request.cookie(cookie)).exchange();
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
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

    @Test
    void theTableCallsTheWaiterSeesWhoIsComingAndTheCallLeavesTheQueue() throws Exception {
        Cookie phone = session(sakura.qr04());

        MvcTestResult created = call(sakura.qr04(), phone, "{\"type\":\"WAITER\"}");
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String id = json(created, "$.id");
        assertThat(json(created, "$.status")).isEqualTo("PENDING");

        MvcTestResult queue = send("GET", sakura.carlos(), "/api/v1/calls", null);
        assertThat(queue).hasStatusOk();
        assertThat(json(queue, "$.content[0].id")).isEqualTo(id);
        assertThat(json(queue, "$.content[0].tableLabel")).isEqualTo("04");
        assertThat(json(queue, "$.content[0].sectorName")).isEqualTo("Salão principal");

        MvcTestResult accepted = send("POST", sakura.carlos(), "/api/v1/calls/" + id + "/accept", null);
        assertThat(accepted).hasStatusOk();
        assertThat(json(accepted, "$.acceptedByName")).isEqualTo("Carlos Souza");

        MvcTestResult tableView = mvc.get().uri(sakura.calls(sakura.qr04())).cookie(phone).exchange();
        assertThat(tableView).hasStatusOk();
        assertThat(json(tableView, "$[0].status")).isEqualTo("IN_PROGRESS");
        assertThat(json(tableView, "$[0].waiterFirstName")).isEqualTo("Carlos");
        assertThat(tableView.getResponse().getContentAsString()).doesNotContain("Souza");

        assertThat(send("POST", sakura.carlos(), "/api/v1/calls/" + id + "/finish", null)).hasStatusOk();
        assertThat(json(send("GET", sakura.carlos(), "/api/v1/calls", null), "$.totalElements")).isEqualTo("0");
        assertThat(json(mvc.get().uri(sakura.calls(sakura.qr04())).cookie(phone).exchange(), "$.length()"))
                .isEqualTo("0");
        // As horas de cada passo ficam guardadas para os relatórios.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM call WHERE id = ?::uuid AND accepted_at IS NOT NULL "
                + "AND done_at IS NOT NULL AND accepted_by IS NOT NULL", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void tappingTwiceKeepsOneCallAndTheBillCanChangeThePaymentMethod() throws Exception {
        Cookie phone = session(sakura.qr04());
        String first = json(call(sakura.qr04(), phone, "{\"type\":\"WAITER\"}"), "$.id");

        MvcTestResult again = call(sakura.qr04(), session(sakura.qr04()), "{\"type\":\"WAITER\"}");
        assertThat(again).hasStatusOk();
        assertThat(json(again, "$.id")).isEqualTo(first);

        assertThat(call(sakura.qr04(), phone, "{\"type\":\"BILL\"}")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(call(sakura.qr04(), phone, "{\"type\":\"BILL\",\"paymentMethod\":\"CARD\"}"))
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult changed = call(sakura.qr04(), phone, "{\"type\":\"BILL\",\"paymentMethod\":\"PIX\"}");
        assertThat(changed).hasStatusOk();
        assertThat(json(changed, "$.paymentMethod")).isEqualTo("PIX");
        assertThat(json(send("GET", sakura.carlos(), "/api/v1/calls", null), "$.totalElements")).isEqualTo("2");
    }

    @Test
    void twoPhonesTappingAtTheSameTimeOpenOnlyOneCall() throws Exception {
        List<Cookie> phones = List.of(session(sakura.qr04()), session(sakura.qr04()), session(sakura.qr04()));

        List<Integer> statuses = together(phones.stream()
                .<Callable<Integer>>map(p -> () -> call(sakura.qr04(), p, "{\"type\":\"WAITER\"}")
                        .getResponse().getStatus())
                .toList());

        assertThat(statuses).containsExactlyInAnyOrder(201, 200, 200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM call WHERE establishment_id = ?", Integer.class,
                sakura.establishmentId())).isEqualTo(1);
    }

    @Test
    void whenTwoWaitersAcceptTogetherOnlyOneGetsIt() throws Exception {
        String id = json(call(sakura.qr04(), session(sakura.qr04()), "{\"type\":\"WAITER\"}"), "$.id");
        String bia = staff(sakura.ownerToken(), sakura.establishmentId(), "Bia Ramos", Role.STAFF);

        List<Integer> statuses = together(List.of(
                () -> send("POST", sakura.carlos(), "/api/v1/calls/" + id + "/accept", null).getResponse().getStatus(),
                () -> send("POST", bia, "/api/v1/calls/" + id + "/accept", null).getResponse().getStatus()));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    void withoutTheSessionOfThisTableTheButtonsAreRefused() throws Exception {
        assertThat(call(sakura.qr04(), null, "{\"type\":\"WAITER\"}")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(call(sakura.qr04(), new Cookie("table_session", "inventado"), "{\"type\":\"WAITER\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        // O cookie da mesa 05 não chama o garçom para a 04.
        assertThat(call(sakura.qr04(), session(sakura.qr05()), "{\"type\":\"WAITER\"}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri(sakura.calls(sakura.qr04())).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(call("qr-que-ninguem-gerou-0000", null, "{\"type\":\"WAITER\"}")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void theTableCancelsItsOwnCallOnly() throws Exception {
        Cookie phone = session(sakura.qr04());
        String id = json(call(sakura.qr04(), phone, "{\"type\":\"WAITER\"}"), "$.id");

        assertThat(mvc.delete().uri(sakura.calls(sakura.qr05()) + "/" + id).cookie(session(sakura.qr05())).exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(sakura.calls(sakura.qr04()) + "/" + id).cookie(phone).exchange())
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.delete().uri(sakura.calls(sakura.qr04()) + "/" + id).cookie(phone).exchange())
                .hasStatus(HttpStatus.CONFLICT);
        assertThat(send("POST", sakura.carlos(), "/api/v1/calls/" + id + "/accept", null))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void theKitchenSeesTheQueueButDoesNotAccept() throws Exception {
        String id = json(call(sakura.qr04(), session(sakura.qr04()), "{\"type\":\"WAITER\"}"), "$.id");
        String kitchen = staff(sakura.ownerToken(), sakura.establishmentId(), "Rui Cozinha", Role.KITCHEN);

        assertThat(send("GET", kitchen, "/api/v1/calls", null)).hasStatusOk();
        assertThat(send("POST", kitchen, "/api/v1/calls/" + id + "/accept", null)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(send("GET", null, "/api/v1/calls", null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Separação entre restaurantes: a equipe do outro não vê, não aceita e não conclui (404). */
    @Test
    void anotherRestaurantNeitherSeesNorTouchesTheCall() throws Exception {
        Restaurant other = newRestaurant("Outro Bar");
        String id = json(call(sakura.qr04(), session(sakura.qr04()), "{\"type\":\"WAITER\"}"), "$.id");

        assertThat(json(send("GET", other.carlos(), "/api/v1/calls", null), "$.totalElements")).isEqualTo("0");
        assertThat(send("POST", other.carlos(), "/api/v1/calls/" + id + "/accept", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        send("POST", sakura.carlos(), "/api/v1/calls/" + id + "/accept", null);
        assertThat(send("POST", other.carlos(), "/api/v1/calls/" + id + "/finish", null))
                .hasStatus(HttpStatus.NOT_FOUND);
        // Nem a mesa do outro restaurante cancela.
        assertThat(mvc.delete().uri(other.calls(other.qr04()) + "/" + id).cookie(session(other.qr04())).exchange())
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    /** Dispara as tarefas juntas (um portão que abre para todas) e devolve os status HTTP. */
    private static List<Integer> together(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<Integer>> futures = tasks.stream().map(task -> pool.submit(() -> {
                gate.await();
                return task.call();
            })).toList();
            gate.countDown();
            return futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        } finally {
            pool.shutdownNow();
        }
    }
}
