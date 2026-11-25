package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.prep;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/** A comparação entre unidades (C8, REL-80): os números lado a lado, a rede, a exportação e quem vê. */
@IntegrationTest
class UnitComparisonControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate day = LocalDate.now(SAO_PAULO).minusDays(2);
    private final LocalDate first = day.minusDays(6);

    private ReportTestData data;
    private Owner pinheiros;
    private Owner moema;

    /**
     * A Sakura (Enterprise) com duas unidades. Pinheiros: R$ 150 em duas comandas (e R$ 60 na semana
     * anterior), preparos de 10 e 20 min, chamados respondidos em 60 e 120 s e 5 avaliações (4,6).
     * Moema (no fuso de Manaus): R$ 300 numa comanda, um preparo de 30 min e 2 avaliações.
     */
    @BeforeEach
    void sakuraWithTwoUnits() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        pinheiros = data.newRestaurant();
        data.onPlan(pinheiros, "enterprise");
        moema = unit(pinheiros, "Sakura Moema", "America/Manaus");

        Place salao = data.place(pinheiros, "Salão");
        UUID prato = data.product(pinheiros, "Prato", 5000);
        data.closedTab(pinheiros, salao, at(day, 19, 0), at(day, 21, 0), 10000, "PIX", 0);
        data.closedTab(pinheiros, salao, at(first, 12, 0), at(first, 13, 0), 5000, "CARD", 0);
        data.closedTab(pinheiros, salao, at(first.minusDays(3), 20, 0), at(first.minusDays(3), 21, 0), 6000, "PIX", 0);
        data.kitchenOrder(pinheiros, salao, at(day, 19, 10), null, prep(prato, "Prato", 10), prep(prato, "Prato", 20));
        UUID carlos = data.waiter(pinheiros, "Carlos");
        UUID phone = data.session(pinheiros, salao, at(day, 19, 0));
        data.call(pinheiros, salao, phone, at(day, 20, 0), at(day, 20, 0).plusSeconds(60), carlos);
        data.call(pinheiros, salao, phone, at(day, 20, 30), at(day, 20, 30).plusSeconds(120), carlos);
        for (int stars : new int[]{5, 5, 4, 4, 5}) {
            data.rating(pinheiros, salao, carlos, stars, null, at(day, 21, 0));
        }

        Place varanda = data.place(moema, "Varanda");
        UUID sushi = data.product(moema, "Sushi", 30000);
        data.closedTab(moema, varanda, at(day, 15, 0), at(day, 16, 0), 30000, "PIX", 0);
        data.kitchenOrder(moema, varanda, at(day, 15, 5), null, prep(sushi, "Sushi", 30));
        data.rating(moema, varanda, null, 4, null, at(day, 16, 0));
        data.rating(moema, varanda, null, 3, null, at(day, 16, 5));
    }

    /** Cria a unidade pela API (como o dono faz) e devolve o dono "entrando" nela. */
    private Owner unit(Owner owner, String name, String timeZone) throws Exception {
        MvcTestResult created = mvc.post().uri("/api/v1/units")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"timeZone\":\"%s\"}".formatted(name, timeZone)).exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        UUID id = UUID.fromString(json(created, "$.id"));
        return new Owner(token(owner.userId(), id, Role.OWNER), owner.userId(), id);
    }

    private String token(UUID user, UUID establishment, Role role) {
        return tokenIssuer.issue(new TokenSubject(new UserId(user), new EstablishmentId(establishment), role)).value();
    }

    private String uri(String path) {
        return "/api/v1/reports/units%s?from=%s&to=%s".formatted(path, first, day);
    }

    private boolean comparisonInContext(String token) throws Exception {
        MvcTestResult context = data.get(token, "/api/v1/reports/context");
        assertThat(context).hasStatusOk();
        return Boolean.parseBoolean(json(context, "$.unitComparison"));
    }

    @Test
    void theUnitsSideBySideAndTheNetwork() throws Exception {
        assertThat(comparisonInContext(pinheiros.token())).isTrue();

        MvcTestResult report = data.get(pinheiros, uri(""));
        assertThat(report).hasStatusOk();
        assertThat(json(report, "$.grouping")).isEqualTo("DAY");
        List<String> names = read(report, "$.units[*].name");
        assertThat(names).hasSize(2).last().asString().startsWith("Restaurante");
        assertThat(names.getFirst()).isEqualTo("Sakura Moema");

        // Moema: a que mais faturou vem primeiro; com 2 avaliações a nota fica de fora.
        assertThat(json(report, "$.units[0].id")).isEqualTo(moema.establishmentId().toString());
        assertThat(json(report, "$.units[0].figures.current.revenueCents")).isEqualTo("30000");
        assertThat(json(report, "$.units[0].figures.prepSeconds")).isEqualTo("1800");
        assertThat(json(report, "$.units[0].figures.callResponseSeconds")).isNull();
        assertThat(json(report, "$.units[0].figures.ratingAverage")).isNull();
        assertThat(json(report, "$.units[0].figures.ratings")).isEqualTo("2");

        assertThat(json(report, "$.units[1].figures.current.revenueCents")).isEqualTo("15000");
        assertThat(json(report, "$.units[1].figures.compared.revenueCents")).isEqualTo("6000");
        assertThat(json(report, "$.units[1].figures.prepSeconds")).isEqualTo("900");
        assertThat(json(report, "$.units[1].figures.callResponseSeconds")).isEqualTo("90");
        assertThat(json(report, "$.units[1].figures.ratingAverage")).isEqualTo("4.6");

        // A evolução: um ponto por dia, com a média por dia.
        List<Integer> moemaDays = read(report, "$.units[0].series[*].revenuePerDayCents");
        assertThat(moemaDays).hasSize(7).contains(30000);
        assertThat(moemaDays.stream().mapToInt(Integer::intValue).sum()).isEqualTo(30000);

        // A rede: as somas e as médias pesadas (preparo: 10, 20 e 30 min = 20 min; nota: 30 estrelas em 7).
        assertThat(json(report, "$.network.current.revenueCents")).isEqualTo("45000");
        assertThat(json(report, "$.network.compared.revenueCents")).isEqualTo("6000");
        assertThat(json(report, "$.network.prepSeconds")).isEqualTo("1200");
        assertThat(json(report, "$.network.callResponseSeconds")).isEqualTo("90");
        assertThat(json(report, "$.network.ratings")).isEqualTo("7");
        assertThat(json(report, "$.network.ratingAverage")).isEqualTo("4.3");

        // De dentro da outra unidade, a mesma comparação.
        MvcTestResult fromMoema = data.get(moema, uri(""));
        assertThat(fromMoema).hasStatusOk();
        assertThat(json(fromMoema, "$.network.current.revenueCents")).isEqualTo("45000");
    }

    @Test
    void theExportHasTheUnitsAndTheEvolution() throws Exception {
        MvcTestResult csv = data.get(pinheiros, uri("/export") + "&format=CSV");
        assertThat(csv).hasStatusOk();
        assertThat(csv.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("ohchefe-unidades-" + first + "-a-" + day + ".csv");
        String body = new String(csv.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(body).contains("Unidades", "Evolução", "Sakura Moema", "Rede", "Faturamento por dia (R$)");

        MvcTestResult xlsx = data.get(pinheiros, uri("/export") + "&format=XLSX");
        assertThat(xlsx).hasStatusOk();
        assertThat(xlsx.getResponse().getContentAsByteArray()).startsWith('P', 'K');
    }

    @Test
    void withoutTheEnterpriseOrWithOneUnitThereIsNoComparison() throws Exception {
        // O gerente não vê (só o dono).
        String manager = token(pinheiros.userId(), pinheiros.establishmentId(), Role.MANAGER);
        assertThat(data.get(manager, uri(""))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(manager, uri("/export"))).hasStatus(HttpStatus.FORBIDDEN);

        // Fora do Enterprise (mesmo com as duas unidades), não existe.
        data.onPlan(pinheiros, "business");
        assertThat(data.get(pinheiros, uri(""))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(data.get(pinheiros, uri("/export"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(comparisonInContext(pinheiros.token())).isFalse();

        // No Enterprise com uma unidade só, também não.
        Owner alone = data.newRestaurant();
        data.onPlan(alone, "enterprise");
        assertThat(data.get(alone, uri(""))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(comparisonInContext(alone.token())).isFalse();
    }

    /** Acesso entre restaurantes: quem não é dono de todas as unidades não vê nenhuma; outra empresa, só a dela. */
    @Test
    void onlyWhoOwnsEveryUnitSeesTheComparison() throws Exception {
        // Uma dona só da Moema (vínculo de dono só ali): nem a comparação, nem os números de Pinheiros.
        UUID partner = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, name) VALUES (?, ?)", partner, "Sócia");
        jdbc.update("INSERT INTO membership (id, user_id, establishment_id, role) VALUES (?, ?, ?, 'OWNER')",
                UUID.randomUUID(), partner, moema.establishmentId());
        String partnerToken = token(partner, moema.establishmentId(), Role.OWNER);
        assertThat(data.get(partnerToken, uri(""))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(data.get(partnerToken, uri("/export"))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(comparisonInContext(partnerToken)).isFalse();

        // O vínculo de dono desativado em Pinheiros também tira a comparação.
        jdbc.update("UPDATE membership SET active = false WHERE user_id = ? AND establishment_id = ?",
                pinheiros.userId(), pinheiros.establishmentId());
        assertThat(data.get(moema, uri(""))).hasStatus(HttpStatus.NOT_FOUND);
        jdbc.update("UPDATE membership SET active = true WHERE user_id = ? AND establishment_id = ?",
                pinheiros.userId(), pinheiros.establishmentId());

        // Outra empresa no Enterprise com duas unidades: compara as dela, nunca as da Sakura.
        Owner other = data.newRestaurant();
        data.onPlan(other, "enterprise");
        Owner otherUnit = unit(other, "Outra Rede Centro", "America/Sao_Paulo");
        MvcTestResult theirs = data.get(other, uri(""));
        assertThat(theirs).hasStatusOk();
        List<String> ids = read(theirs, "$.units[*].id");
        assertThat(ids).containsExactlyInAnyOrder(other.establishmentId().toString(),
                otherUnit.establishmentId().toString());
        assertThat(json(theirs, "$.network.current.revenueCents")).isEqualTo("0");
    }
}
