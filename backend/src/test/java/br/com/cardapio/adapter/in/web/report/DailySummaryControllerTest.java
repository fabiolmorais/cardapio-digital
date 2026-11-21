package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.in.report.SendDailySummariesUseCase;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.report.model.SummaryWindow;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/** O resumo diário (R4): a configuração, a prévia, o teste e o envio agendado. */
@IntegrationTest
class DailySummaryControllerTest {

    private static final String SETTINGS = "/api/v1/reports/daily-summary/settings";
    private static final String PREVIEW = "/api/v1/reports/daily-summary/preview";
    private static final String TEST = "/api/v1/reports/daily-summary/test";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SendDailySummariesUseCase job;

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private UUID prato;
    private UUID manager;

    @BeforeEach
    void sakuraWithAManager() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        prato = data.product(sakura, "Prato da casa", 5000);
        manager = member(sakura, "Rodrigo", "MANAGER", null);
        member(sakura, "Ana do salão", "STAFF", null);
    }

    /** Alguém da equipe, entrando pelo PIN (sem e-mail). */
    private UUID member(Owner owner, String name, String role, String email) {
        UUID user = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user (id, name) VALUES (?, ?)", user, name);
        jdbc.update("""
                INSERT INTO membership (id, user_id, establishment_id, role, pin_hash) VALUES (?, ?, ?, ?, 'x')
                """, UUID.randomUUID(), user, owner.establishmentId(), role);
        return user;
    }

    /** Uma comanda de R$ 50,00 fechada no meio do último dia que já terminou (no horário das 2h). */
    private void aTabInTheLastDay(Owner owner, Place place) {
        SummaryWindow window = SummaryWindow.lastEnded(Instant.now(), 2, SAO_PAULO);
        Instant closed = window.start().plus(Duration.ofHours(18));
        data.closedTab(owner, place, closed.minus(Duration.ofHours(1)), closed, 5000, "PIX", 0,
                item(prato, "Prato da casa", 5000, 1));
    }

    private MvcTestResult put(String token, String body) {
        return mvc.put().uri(SETTINGS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult post(String token, String uri) {
        return mvc.post().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }

    @Test
    void startsOnAtTwoForTheOwnerAndTheManagers() throws Exception {
        MvcTestResult result = data.get(sakura, SETTINGS);

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.emailEnabled")).isEqualTo("true");
        assertThat(json(result, "$.pushEnabled")).isEqualTo("true");
        assertThat(json(result, "$.sendHour")).isEqualTo("2");
        // O dono primeiro; quem é do salão não entra.
        List<String> names = read(result, "$.recipients[*].name");
        assertThat(names).containsExactly("Marina", "Rodrigo");
        assertThat(json(result, "$.recipients[0].role")).isEqualTo("OWNER");
        assertThat(json(result, "$.recipients[0].hasEmail")).isEqualTo("true");
        assertThat(json(result, "$.recipients[1].hasEmail")).isEqualTo("false");
        assertThat(json(result, "$.recipients[1].receives")).isEqualTo("true");
    }

    @Test
    void theOwnerChangesTheHourTheChannelsAndWhoReceives() throws Exception {
        MvcTestResult saved = put(sakura.token(), """
                {"emailEnabled":true,"pushEnabled":false,"sendHour":8,"receiverIds":["%s"]}
                """.formatted(sakura.userId()));

        assertThat(saved).hasStatusOk();
        MvcTestResult again = data.get(sakura, SETTINGS);
        assertThat(json(again, "$.pushEnabled")).isEqualTo("false");
        assertThat(json(again, "$.sendHour")).isEqualTo("8");
        assertThat(json(again, "$.recipients[0].receives")).isEqualTo("true");
        assertThat(json(again, "$.recipients[1].receives")).isEqualTo("false");

        // Ninguém recebe: vale também.
        assertThat(put(sakura.token(), """
                {"emailEnabled":true,"pushEnabled":true,"sendHour":2,"receiverIds":[]}
                """)).hasStatusOk();
        assertThat(json(data.get(sakura, SETTINGS), "$.recipients[0].receives")).isEqualTo("false");
    }

    @Test
    void theHourGoesFromZeroToTwentyThree() {
        assertThat(put(sakura.token(), """
                {"emailEnabled":true,"pushEnabled":true,"sendHour":24,"receiverIds":[]}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(sakura.token(), """
                {"emailEnabled":true,"pushEnabled":true,"receiverIds":[]}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void thePreviewIsTheLastDayThatEnded() throws Exception {
        aTabInTheLastDay(sakura, salao);

        MvcTestResult result = data.get(sakura, PREVIEW);

        assertThat(result).hasStatusOk();
        assertThat(json(result, "$.day")).isEqualTo(
                SummaryWindow.lastEnded(Instant.now(), 2, SAO_PAULO).day().toString());
        assertThat(json(result, "$.current.revenueCents")).isEqualTo("5000");
        assertThat(json(result, "$.current.tabs")).isEqualTo("1");
        assertThat(json(result, "$.compared.tabs")).isEqualTo("0");
        assertThat(json(result, "$.champion.name")).isEqualTo("Prato da casa");
        assertThat(json(result, "$.payments[0].method")).isEqualTo("PIX");
    }

    @Test
    void theTestGoesOnlyToWhoAsked() throws Exception {
        MvcTestResult owner = post(sakura.token(), TEST);
        MvcTestResult byManager = post(data.token(sakura, Role.MANAGER), TEST);

        assertThat(owner).hasStatusOk();
        assertThat(json(owner, "$.email")).isEqualTo("true");
        assertThat(json(owner, "$.pushDevices")).isEqualTo("0");
        // O token de gerente acima leva o usuário do dono; um gerente de verdade, que entra pelo PIN, não tem e-mail.
        String realManager = tokenIssuer.issue(new TokenSubject(new UserId(manager),
                new EstablishmentId(sakura.establishmentId()), Role.MANAGER)).value();
        MvcTestResult pinOnly = post(realManager, TEST);
        assertThat(byManager).hasStatusOk();
        assertThat(json(pinOnly, "$.email")).isEqualTo("false");
    }

    @Test
    void theJobSendsEachDayOnceAndSkipsEmptyDaysAndTheEssentialPlan() throws Exception {
        aTabInTheLastDay(sakura, salao);
        Owner quiet = data.newRestaurant();
        Owner essential = data.newRestaurant();
        Place theirs = data.place(essential, "Salão");
        data.onPlan(essential, "essential");
        UUID theirPrato = data.product(essential, "Pizza grande", 5000);
        SummaryWindow window = SummaryWindow.lastEnded(Instant.now(), 2, SAO_PAULO);
        Instant closed = window.start().plus(Duration.ofHours(18));
        data.closedTab(essential, theirs, closed.minus(Duration.ofHours(1)), closed, 5000, "PIX", 0,
                item(theirPrato, "Pizza grande", 5000, 1));

        runJob();

        assertThat(lastSent(sakura)).isEqualTo(window.day());
        // O dia sem movimento também é marcado (não gera resumo, e não é conferido de novo).
        assertThat(lastSent(quiet)).isEqualTo(window.day());
        assertThat(lastSent(essential)).isNull();
        // Na rodada seguinte, o mesmo dia não sai de novo.
        jdbc.update("UPDATE daily_summary_setting SET updated_at = now() WHERE establishment_id = ?",
                sakura.establishmentId());
        runJob();
        assertThat(lastSent(sakura)).isEqualTo(window.day());
    }

    @Test
    void turnedOffItIsNotSent() throws Exception {
        aTabInTheLastDay(sakura, salao);
        assertThat(put(sakura.token(), """
                {"emailEnabled":false,"pushEnabled":false,"sendHour":2,"receiverIds":["%s"]}
                """.formatted(sakura.userId()))).hasStatusOk();

        runJob();

        assertThat(lastSent(sakura)).isNull();
    }

    /** O que a tarefa agendada faz: quem está na vez, um por vez. */
    private void runJob() {
        job.due().forEach(job::send);
    }

    private LocalDate lastSent(Owner owner) {
        List<LocalDate> days = jdbc.query("SELECT last_sent_day FROM daily_summary_setting WHERE establishment_id = ?",
                (rs, row) -> rs.getObject(1, LocalDate.class), owner.establishmentId());
        return days.isEmpty() ? null : days.getFirst();
    }

    @Test
    void anotherRestaurantsPeopleCannotBeChosen() throws Exception {
        Owner other = data.newRestaurant();
        UUID staff = jdbc.queryForObject("""
                SELECT user_id FROM membership WHERE establishment_id = ? AND role = 'STAFF'
                """, UUID.class, sakura.establishmentId());

        // O dono de outro restaurante, e alguém do salão deste: 404.
        assertThat(put(sakura.token(), """
                {"emailEnabled":true,"pushEnabled":true,"sendHour":2,"receiverIds":["%s"]}
                """.formatted(other.userId()))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(put(sakura.token(), """
                {"emailEnabled":true,"pushEnabled":true,"sendHour":2,"receiverIds":["%s"]}
                """.formatted(staff))).hasStatus(HttpStatus.NOT_FOUND);
        // E nada mudou, nem lá.
        assertThat(json(data.get(sakura, SETTINGS), "$.recipients[0].receives")).isEqualTo("true");
        List<String> theirs = read(data.get(other, SETTINGS), "$.recipients[*].name");
        assertThat(theirs).containsExactly("Marina");
        // A prévia de cada um só vê as próprias comandas.
        aTabInTheLastDay(sakura, salao);
        assertThat(json(data.get(other, PREVIEW), "$.current.tabs")).isEqualTo("0");
    }

    @Test
    void onlyTheOwnerAndTheManagerWithReportsInThePlan() {
        for (Role role : new Role[]{Role.STAFF, Role.KITCHEN}) {
            String token = data.token(sakura, role);
            assertThat(data.get(token, SETTINGS)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(data.get(token, PREVIEW)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(post(token, TEST)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(put(token, "{\"emailEnabled\":true,\"pushEnabled\":true,\"sendHour\":2,\"receiverIds\":[]}"))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
        data.onPlan(sakura, "essential");
        assertThat(data.get(sakura, SETTINGS)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(sakura, PREVIEW)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(sakura.token(), TEST)).hasStatus(HttpStatus.FORBIDDEN);
    }
}
