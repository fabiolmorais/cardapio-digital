package br.com.cardapio.adapter.in.web.plan;

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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class SubscriptionControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String SUBSCRIPTION = "/api/v1/subscription";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    record Owner(String token, UUID userId, UUID establishmentId) {
    }

    private Owner sakura;

    @BeforeEach
    void ownerOfSakura() throws Exception {
        sakura = newRestaurant();
    }

    private Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(send("POST", null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = send("POST", null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.userId")),
                UUID.fromString(json(login, "$.establishment.id")));
    }

    private MvcTestResult send(String method, String token, String uri, String body) {
        var request = switch (method) {
            case "POST" -> mvc.post().uri(uri);
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
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    private String create(String token, String uri, String body) throws Exception {
        MvcTestResult created = send("POST", token, uri, body);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return json(created, "$.id");
    }

    /** Muda o plano do restaurante direto no banco (a troca pelo dono vem no próximo guia). */
    private void onPlan(Owner owner, String code) {
        jdbc.update("""
                UPDATE subscription SET plan_id = (SELECT id FROM plan WHERE code = ?)
                WHERE organization_id = (SELECT organization_id FROM establishment WHERE id = ?)
                """, code, owner.establishmentId());
    }

    @Test
    void everySignupStartsOnAFourteenDayProTrial() throws Exception {
        MvcTestResult result = send("GET", sakura.token(), SUBSCRIPTION, null);

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.plan.code")).isEqualTo("pro");
        assertThat(json(result, "$.standing")).isEqualTo("TRIAL");
        Instant coveredUntil = Instant.parse(json(result, "$.coveredUntil"));
        assertThat(Duration.between(Instant.now(), coveredUntil)).isBetween(Duration.ofDays(14).minusMinutes(5),
                Duration.ofDays(14));
        assertThat(Instant.parse(json(result, "$.graceEndsAt"))).isEqualTo(coveredUntil.plus(Duration.ofDays(7)));
        // Só o dono, ainda sem salão.
        assertThat(json(result, "$.usage.users")).isEqualTo("1");
        assertThat(json(result, "$.usage.tables")).isEqualTo("0");
        assertThat(json(result, "$.usage.sectors")).isEqualTo("0");
    }

    @Test
    void theEssentialPlanStopsTheTwentyFirstTableTheSecondSectorAndVideos() throws Exception {
        onPlan(sakura, "essential");
        String salao = create(sakura.token(), "/api/v1/sectors", "{\"name\":\"Salão\"}");
        String last = null;
        for (int i = 1; i <= 20; i++) {
            last = create(sakura.token(), "/api/v1/tables", "{\"label\":\"%02d\",\"sectorId\":\"%s\"}".formatted(i, salao));
        }

        MvcTestResult table21 = send("POST", sakura.token(), "/api/v1/tables",
                "{\"label\":\"21\",\"sectorId\":\"%s\"}".formatted(salao));
        assertThat(table21).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(table21, "$.detail"))
                .isEqualTo("O plano Essencial permite até 20 mesas ativas. Para ter mais, mude de plano.");
        assertThat(send("POST", sakura.token(), "/api/v1/sectors", "{\"name\":\"Deck\"}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

        String category = create(sakura.token(), "/api/v1/menu-categories", "{\"name\":\"Entradas\"}");
        String guioza = create(sakura.token(), "/api/v1/products", """
                {"categoryId":"%s","name":"Guioza","priceCents":2990,"featured":false,"available":true}
                """.formatted(category));
        MvcTestResult video = send("POST", sakura.token(), "/api/v1/products/" + guioza + "/videos",
                "{\"contentType\":\"video/mp4\",\"sizeBytes\":1000000}");
        assertThat(video).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(video, "$.detail")).startsWith("Vídeos nos pratos fazem parte dos planos Pro");

        // Desativar libera a vaga: a 21ª entra.
        assertThat(send("PATCH", sakura.token(), "/api/v1/tables/" + last, "{\"active\":false}")).hasStatusOk();
        create(sakura.token(), "/api/v1/tables", "{\"label\":\"21\",\"sectorId\":\"%s\"}".formatted(salao));
        assertThat(json(send("GET", sakura.token(), SUBSCRIPTION, null), "$.usage.tables")).isEqualTo("20");
    }

    @Test
    void theEssentialPlanHasFivePeopleCountingTheOwner() throws Exception {
        onPlan(sakura, "essential");
        String[] pins = {"258014", "369027", "471852", "582963"};
        for (int i = 0; i < 4; i++) {
            create(sakura.token(), "/api/v1/team-members",
                    "{\"name\":\"Garçom %d\",\"role\":\"STAFF\",\"pin\":\"%s\"}".formatted(i, pins[i]));
        }

        MvcTestResult sixth = send("POST", sakura.token(), "/api/v1/team-members",
                "{\"name\":\"Rita\",\"role\":\"KITCHEN\",\"pin\":\"258014\"}");
        assertThat(sixth).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(json(sixth, "$.detail")).contains("até 5 pessoas ativas na equipe");
    }

    @Test
    void onlyTheOwnerAndTheManagerSeeTheSubscription() {
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            String token = tokenIssuer.issue(new TokenSubject(new UserId(sakura.userId()),
                    new EstablishmentId(sakura.establishmentId()), role)).value();
            assertThat(send("GET", token, SUBSCRIPTION, null)).hasStatus(HttpStatus.FORBIDDEN);
        }
        assertThat(send("GET", null, SUBSCRIPTION, null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Acesso entre restaurantes: cada dono vê a assinatura e o uso do próprio restaurante. */
    @Test
    void eachRestaurantSeesOnlyItsOwnPlanAndUsage() throws Exception {
        Owner tanuki = newRestaurant();
        onPlan(tanuki, "essential");
        String deck = create(tanuki.token(), "/api/v1/sectors", "{\"name\":\"Deck\"}");
        create(tanuki.token(), "/api/v1/tables", "{\"label\":\"01\",\"sectorId\":\"%s\"}".formatted(deck));

        MvcTestResult mine = send("GET", sakura.token(), SUBSCRIPTION, null);
        assertThat(json(mine, "$.plan.code")).isEqualTo("pro");
        assertThat(json(mine, "$.usage.tables")).isEqualTo("0");
        assertThat(json(mine, "$.usage.sectors")).isEqualTo("0");
        MvcTestResult theirs = send("GET", tanuki.token(), SUBSCRIPTION, null);
        assertThat(json(theirs, "$.plan.code")).isEqualTo("essential");
        assertThat(json(theirs, "$.usage.tables")).isEqualTo("1");
    }
}
