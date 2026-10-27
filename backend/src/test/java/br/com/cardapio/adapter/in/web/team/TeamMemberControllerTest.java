package br.com.cardapio.adapter.in.web.team;

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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class TeamMemberControllerTest {

    private static final String PASSWORD = "sushi2026bom";
    private static final String TEAM = "/api/v1/team-members";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    /** Dono de um restaurante: token e id do restaurante. */
    record Owner(String token, UUID establishmentId) {
    }

    private Owner sakura;

    @BeforeEach
    void ownerOfSakura() throws Exception {
        sakura = newRestaurant();
    }

    /** Cria um restaurante pela API e entra como dono. */
    private Owner newRestaurant() throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String signup = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), email, PASSWORD);
        assertThat(post(null, "/api/v1/signup", signup)).hasStatus(HttpStatus.CREATED);
        MvcTestResult login = post(null, "/api/v1/auth/login",
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        return new Owner(json(login, "$.accessToken"), UUID.fromString(json(login, "$.establishment.id")));
    }

    /** Token assinado de verdade para alguém da equipe (o login por PIN vem no próximo guia). */
    private String tokenFor(String memberId, UUID establishmentId, Role role) {
        UUID userId = jdbc.queryForObject("SELECT user_id FROM membership WHERE id = ?", UUID.class,
                UUID.fromString(memberId));
        return tokenIssuer.issue(new TokenSubject(new UserId(userId), new EstablishmentId(establishmentId), role))
                .value();
    }

    private MvcTestResult post(String token, String uri, String body) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).exchange();
    }

    private MvcTestResult patch(String token, String id, String body) {
        return mvc.patch().uri(TEAM + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private MvcTestResult get(String token, String uri) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String token, String id) {
        return mvc.delete().uri(TEAM + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    /** Cadastra alguém e devolve o id. */
    private String add(String token, String name, Role role) throws Exception {
        MvcTestResult added = post(token, TEAM, """
                {"name":"%s","role":"%s","pin":"2580"}""".formatted(name, role));
        assertThat(added).hasStatus(HttpStatus.CREATED);
        return json(added, "$.id");
    }

    @Test
    void ownerAddsListsUpdatesAndDeactivatesAMember() throws Exception {
        MvcTestResult added = post(sakura.token(), TEAM, """
                {"name":"Carlos Souza","role":"STAFF","pin":"2580"}""");

        assertThat(added).hasStatus(HttpStatus.CREATED);
        String carlos = json(added, "$.id");
        assertThat(added.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(TEAM + "/" + carlos);
        assertThat(added).bodyJson().extractingPath("$.role").isEqualTo("STAFF");
        assertThat(added).bodyJson().extractingPath("$.active").isEqualTo(true);
        assertThat(added.getResponse().getContentAsString()).doesNotContain("2580", "pin");

        // No banco: PIN só como hash BCrypt, e a pessoa não tem e-mail nem senha.
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT m.pin_hash, u.email, u.password_hash FROM membership m JOIN app_user u ON u.id = m.user_id
                WHERE m.id = ?""", UUID.fromString(carlos));
        assertThat((String) row.get("pin_hash")).startsWith("$2").doesNotContain("2580");
        assertThat(row.get("email")).isNull();
        assertThat(row.get("password_hash")).isNull();

        MvcTestResult list = get(sakura.token(), TEAM);
        assertThat(list).hasStatusOk();
        assertThat(list).bodyJson().extractingPath("$.content[*].name").asArray().containsExactly("Carlos Souza", "Marina");
        assertThat(list).bodyJson().extractingPath("$.totalElements").isEqualTo(2);

        MvcTestResult updated = patch(sakura.token(), carlos, "{\"role\":\"KITCHEN\",\"pin\":\"7391\"}");
        assertThat(updated).hasStatusOk();
        assertThat(updated).bodyJson().extractingPath("$.role").isEqualTo("KITCHEN");

        assertThat(delete(sakura.token(), carlos)).hasStatus(HttpStatus.NO_CONTENT);
        MvcTestResult afterDelete = get(sakura.token(), TEAM + "/" + carlos);
        assertThat(afterDelete).hasStatusOk();
        assertThat(afterDelete).bodyJson().extractingPath("$.active").isEqualTo(false);
        // Desligados vão para o fim da lista.
        assertThat(get(sakura.token(), TEAM)).bodyJson().extractingPath("$.content[*].name").asArray()
                .containsExactly("Marina", "Carlos Souza");

        // Religar devolve a mesma pessoa, com o papel que ela tinha.
        MvcTestResult back = post(sakura.token(), TEAM + "/" + carlos + "/reactivation", "");
        assertThat(back).hasStatusOk();
        assertThat(back).bodyJson().extractingPath("$.id").isEqualTo(carlos);
        assertThat(back).bodyJson().extractingPath("$.role").isEqualTo("KITCHEN");
        assertThat(back).bodyJson().extractingPath("$.active").isEqualTo(true);
    }

    @Test
    void invalidDataGets400() throws Exception {
        MvcTestResult missing = post(sakura.token(), TEAM, "{\"role\":\"STAFF\",\"pin\":\"25a0\"}");
        assertThat(missing).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(missing).bodyJson().extractingPath("$.fields").asArray().containsExactly("name", "pin");

        MvcTestResult weakPin = post(sakura.token(), TEAM, "{\"name\":\"Carlos\",\"role\":\"STAFF\",\"pin\":\"1234\"}");
        assertThat(weakPin).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(json(weakPin, "$.detail")).contains("fácil de adivinhar");
    }

    @Test
    void withoutTokenGets401() {
        assertThat(mvc.get().uri(TEAM).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void waiterAndKitchenCannotSeeTheTeam() throws Exception {
        String carlos = add(sakura.token(), "Carlos", Role.STAFF);
        String waiter = tokenFor(carlos, sakura.establishmentId(), Role.STAFF);

        assertThat(get(waiter, TEAM)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(waiter, TEAM, "{\"name\":\"Rita\",\"role\":\"KITCHEN\",\"pin\":\"2580\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void managerManagesWaitersButNotManagersNorTheOwner() throws Exception {
        String ana = add(sakura.token(), "Ana", Role.MANAGER);
        String manager = tokenFor(ana, sakura.establishmentId(), Role.MANAGER);
        String owner = jdbc.queryForObject("SELECT id FROM membership WHERE establishment_id = ? AND role = 'OWNER'",
                UUID.class, sakura.establishmentId()).toString();

        String carlos = add(manager, "Carlos", Role.STAFF);
        assertThat(patch(manager, carlos, "{\"role\":\"KITCHEN\"}")).hasStatusOk();

        assertThat(post(manager, TEAM, "{\"name\":\"Bia\",\"role\":\"MANAGER\",\"pin\":\"2580\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(patch(manager, carlos, "{\"role\":\"MANAGER\"}")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(patch(manager, ana, "{\"pin\":\"7391\"}")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(delete(manager, owner)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(manager, TEAM + "/" + ana + "/reactivation", "")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(delete(sakura.token(), owner)).hasStatus(HttpStatus.FORBIDDEN);
    }

    /**
     * Acesso entre restaurantes (CLAUDE.md): o dono de outro restaurante recebe 404 em tudo,
     * como se a pessoa não existisse, e nada muda nela.
     */
    @Test
    void ownerOfAnotherRestaurantGets404AndChangesNothing() throws Exception {
        String carlos = add(sakura.token(), "Carlos", Role.STAFF);
        Owner other = newRestaurant();

        assertThat(get(other.token(), TEAM + "/" + carlos)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(patch(other.token(), carlos, "{\"role\":\"KITCHEN\",\"pin\":\"7391\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(delete(other.token(), carlos)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(post(other.token(), TEAM + "/" + carlos + "/reactivation", "")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(other.token(), TEAM)).bodyJson().extractingPath("$.content[*].id").asArray()
                .doesNotContain(carlos);

        MvcTestResult stillTheSame = get(sakura.token(), TEAM + "/" + carlos);
        assertThat(stillTheSame).bodyJson().extractingPath("$.role").isEqualTo("STAFF");
        assertThat(stillTheSame).bodyJson().extractingPath("$.active").isEqualTo(true);
    }
}