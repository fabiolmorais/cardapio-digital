package br.com.cardapio.adapter.in.web.unit;

import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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

import static org.assertj.core.api.Assertions.assertThat;

/** Várias unidades (RF31): o cadastro, o limite do plano, a troca de unidade e o isolamento. */
@IntegrationTest
class UnitControllerTest {

    private static final String PASSWORD = "sushi2026bom";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    /** Uma conta nova; devolve o login (token, restaurante e o cookie). */
    private MvcTestResult account(String plan) throws Exception {
        String email = "dono-" + UUID.randomUUID() + "@teste.com";
        String body = """
                {"establishmentName":"Sakura %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"%s","password":"%s","acceptedTerms":true}
                """.formatted(UUID.randomUUID().toString().substring(0, 8), email, PASSWORD);
        assertThat(mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON).content(body).exchange())
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult login = login(email);
        jdbc.update("""
                UPDATE subscription SET plan_id = (SELECT id FROM plan WHERE code = ?)
                 WHERE organization_id = (SELECT organization_id FROM establishment WHERE id = ?)
                """, plan, UUID.fromString(json(login, "$.establishment.id")));
        return login;
    }

    private MvcTestResult login(String email) {
        return mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)).exchange();
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return String.valueOf((Object) JsonPath.read(result.getResponse().getContentAsString(), path));
    }

    private static String bearer(MvcTestResult login) throws Exception {
        return "Bearer " + json(login, "$.accessToken");
    }

    private MvcTestResult createUnit(String bearer, String name, String timeZone) {
        return mvc.post().uri("/api/v1/units").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"timeZone\":\"%s\"}".formatted(name, timeZone)).exchange();
    }

    private MvcTestResult switchTo(String bearer, String refresh, String establishmentId) {
        var request = mvc.post().uri("/api/v1/auth/switch").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"establishmentId\":\"%s\"}".formatted(establishmentId));
        if (refresh != null) {
            request = request.cookie(new Cookie("refresh_token", refresh));
        }
        return request.exchange();
    }

    private static String cookie(MvcTestResult result) {
        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        return cookie.getValue();
    }

    @Test
    void theEnterpriseOwnerCreatesAUnitAndSeesItInTheList() throws Exception {
        MvcTestResult owner = account("enterprise");

        MvcTestResult created = createUnit(bearer(owner), "Sakura Moema", "America/Manaus");
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.name").isEqualTo("Sakura Moema");
        assertThat(created).bodyJson().extractingPath("$.slug").asString().startsWith("sakura-moema");
        assertThat(created).bodyJson().extractingPath("$.activeTables").isEqualTo(0);
        assertThat(created).bodyJson().extractingPath("$.activeTeam").isEqualTo(1);
        assertThat(created).bodyJson().extractingPath("$.current").isEqualTo(false);
        String unitId = json(created, "$.id");
        assertThat(jdbc.queryForObject("SELECT time_zone FROM establishment WHERE id = ?", String.class,
                UUID.fromString(unitId))).isEqualTo("America/Manaus");

        MvcTestResult list = mvc.get().uri("/api/v1/units").header(HttpHeaders.AUTHORIZATION, bearer(owner)).exchange();
        assertThat(list).hasStatusOk();
        assertThat(list).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
        List<String> ids = JsonPath.read(list.getResponse().getContentAsString(), "$.content[*].id");
        assertThat(ids).containsExactlyInAnyOrder(json(owner, "$.establishment.id"), unitId);
        List<Boolean> current = JsonPath.read(list.getResponse().getContentAsString(),
                "$.content[?(@.id == '" + json(owner, "$.establishment.id") + "')].current");
        assertThat(current).containsExactly(true);

        MvcTestResult allowance = mvc.get().uri("/api/v1/units/allowance")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner)).exchange();
        assertThat(allowance).bodyJson().extractingPath("$.planName").isEqualTo("Enterprise");
        assertThat(allowance).bodyJson().extractingPath("$.used").isEqualTo(2);
        assertThat(allowance).bodyJson().extractingPath("$.max").isNull();
        assertThat(allowance).bodyJson().extractingPath("$.canAddMore").isEqualTo(true);
    }

    @Test
    void outsideTheEnterpriseTheSecondUnitIsBlockedByThePlan() throws Exception {
        MvcTestResult owner = account("business");

        MvcTestResult allowance = mvc.get().uri("/api/v1/units/allowance")
                .header(HttpHeaders.AUTHORIZATION, bearer(owner)).exchange();
        assertThat(allowance).bodyJson().extractingPath("$.max").isEqualTo(1);
        assertThat(allowance).bodyJson().extractingPath("$.canAddMore").isEqualTo(false);

        MvcTestResult blocked = createUnit(bearer(owner), "Sakura Moema", "America/Sao_Paulo");
        assertThat(blocked).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(blocked).bodyJson().extractingPath("$.detail")
                .isEqualTo("O plano Business permite até 1 unidade ativa. Para ter mais, mude de plano.");
    }

    @Test
    void invalidInputAndOtherRolesAreRefused() throws Exception {
        MvcTestResult owner = account("enterprise");
        assertThat(createUnit(bearer(owner), "Sakura Moema", "Europe/Lisbon")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(createUnit(bearer(owner), " ", "America/Sao_Paulo")).hasStatus(HttpStatus.BAD_REQUEST);

        // O gerente (mesmo do Enterprise) não vê nem cria unidades.
        String manager = "Bearer " + tokenIssuer.issue(new TokenSubject(
                new UserId(UUID.fromString(json(owner, "$.userId"))),
                new EstablishmentId(UUID.fromString(json(owner, "$.establishment.id"))), Role.MANAGER)).value();
        assertThat(createUnit(manager, "Sakura Moema", "America/Sao_Paulo")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/v1/units").header(HttpHeaders.AUTHORIZATION, manager).exchange())
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void switchingOpensASessionInTheOtherUnitAndEndsTheOldOne() throws Exception {
        MvcTestResult owner = account("enterprise");
        String home = json(owner, "$.establishment.id");
        String unitId = json(createUnit(bearer(owner), "Sakura Moema", "America/Sao_Paulo"), "$.id");
        String oldCookie = cookie(owner);

        MvcTestResult switched = switchTo(bearer(owner), oldCookie, unitId);
        assertThat(switched).hasStatusOk();
        assertThat(switched).bodyJson().extractingPath("$.establishment.id").isEqualTo(unitId);
        assertThat(switched).bodyJson().extractingPath("$.establishment.name").isEqualTo("Sakura Moema");
        assertThat(switched).bodyJson().extractingPath("$.establishment.role").isEqualTo("OWNER");

        // O token novo é da unidade nova: o /me e a lista de unidades acompanham.
        MvcTestResult me = mvc.get().uri("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(switched)).exchange();
        assertThat(me).bodyJson().extractingPath("$.establishment.id").isEqualTo(unitId);
        List<String> mine = JsonPath.read(me.getResponse().getContentAsString(), "$.establishments[*].name");
        assertThat(mine).hasSize(2).contains("Sakura Moema");

        // O cookie antigo não renova mais; o novo renova na unidade nova.
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", oldCookie)).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        MvcTestResult renewed = mvc.post().uri("/api/v1/auth/refresh")
                .cookie(new Cookie("refresh_token", cookie(switched))).exchange();
        assertThat(renewed).bodyJson().extractingPath("$.establishment.id").isEqualTo(unitId);

        // O próximo login entra na unidade usada por último; e dá para voltar para a primeira.
        MvcTestResult again = login(json(me, "$.email"));
        assertThat(again).bodyJson().extractingPath("$.establishment.id").isEqualTo(unitId);
        assertThat(switchTo(bearer(again), null, home)).bodyJson().extractingPath("$.establishment.id").isEqualTo(home);
    }

    /** Acesso entre restaurantes: a unidade de outra empresa não aparece e não abre. */
    @Test
    void anotherCompanysUnitsAreInvisible() throws Exception {
        MvcTestResult marina = account("enterprise");
        MvcTestResult other = account("enterprise");
        String otherUnit = json(createUnit(bearer(other), "Outra Rede Centro", "America/Sao_Paulo"), "$.id");

        assertThat(switchTo(bearer(marina), cookie(marina), otherUnit)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(switchTo(bearer(marina), null, json(other, "$.establishment.id"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(switchTo(bearer(marina), null, UUID.randomUUID().toString())).hasStatus(HttpStatus.NOT_FOUND);
        // O cookie de quem tentou continua valendo: a troca recusada não derruba a sessão.
        assertThat(mvc.post().uri("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", cookie(marina))).exchange())
                .hasStatusOk();

        MvcTestResult list = mvc.get().uri("/api/v1/units").header(HttpHeaders.AUTHORIZATION, bearer(marina)).exchange();
        assertThat(list).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
        assertThat(list).bodyJson().extractingPath("$.content[0].id").isEqualTo(json(marina, "$.establishment.id"));

        // A unidade nova é separada em tudo: a equipe e as mesas da outra empresa não vazam.
        MvcTestResult otherList = mvc.get().uri("/api/v1/units").header(HttpHeaders.AUTHORIZATION, bearer(other)).exchange();
        assertThat(otherList).bodyJson().extractingPath("$.totalElements").isEqualTo(2);
    }
}
