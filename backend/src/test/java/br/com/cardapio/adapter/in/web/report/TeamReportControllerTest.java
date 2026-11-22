package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportTestData.Owner;
import br.com.cardapio.adapter.in.web.report.ReportTestData.Place;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.cardapio.adapter.in.web.report.ReportTestData.SAO_PAULO;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.at;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.item;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.json;
import static br.com.cardapio.adapter.in.web.report.ReportTestData.read;
import static org.assertj.core.api.Assertions.assertThat;

/** Atendimento e equipe (R9): chamados, garçons, por hora, por tipo, mesas, trava de repetidos e turnos. */
@IntegrationTest
class TeamReportControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AccessTokenIssuer tokenIssuer;

    @Autowired
    JdbcTemplate jdbc;

    private final LocalDate day = LocalDate.now(SAO_PAULO).minusDays(2);
    private final LocalDate first = day.minusDays(6);

    private ReportTestData data;
    private Owner sakura;
    private Place salao;
    private UUID carlos;

    /**
     * No último dia, às 20h: 3 comandas fechadas na mesa Sa1, a de R$ 50 fechada pelo Carlos (que entregou
     * os 2 pratos e ficou com R$ 5 de serviço). Chamados: Carlos aceita dois (60 e 120 s), Ana um (30 s),
     * um cancelado pelo cliente e um "pedir a conta" 10 min antes de a comanda fechar. O Carlos estava no
     * turno das 19h às 23h. 3 avisos de pedido repetido, 1 confirmado.
     */
    @BeforeEach
    void sakuraWithAWaiterNight() throws Exception {
        data = new ReportTestData(mvc, jdbc, tokenIssuer);
        sakura = data.newRestaurant();
        salao = data.place(sakura, "Salão");
        UUID prato = data.product(sakura, "Prato da casa", 2500);
        carlos = data.waiter(sakura, "Carlos");
        UUID ana = data.waiter(sakura, "Ana");

        UUID dinner = data.closedTab(sakura, salao, at(day, 20, 0), at(day, 21, 30), 5000, "PIX", 500,
                item(prato, "Prato da casa", 2500, 2));
        jdbc.update("""
                UPDATE tab SET closed_by = ?, service_destination = 'WAITER', service_waiter_id = ?, service_accepted = true
                WHERE id = ?
                """, carlos, carlos, dinner);
        jdbc.update("UPDATE order_item SET delivered_by = ? WHERE order_id IN (SELECT id FROM customer_order WHERE tab_id = ?)",
                carlos, dinner);
        data.closedTab(sakura, salao, at(day, 12, 0), at(day, 13, 0), 2500, "PIX", 0, item(prato, "Prato da casa", 2500, 1));
        data.closedTab(sakura, salao, at(first, 12, 0), at(first, 13, 0), 2500, "PIX", 0,
                item(prato, "Prato da casa", 2500, 1));

        UUID phone = data.session(sakura, salao, at(day, 20, 0));
        data.call(sakura, salao, phone, at(day, 20, 15), at(day, 20, 16), carlos);
        data.call(sakura, salao, phone, at(day, 20, 30), at(day, 20, 32), carlos);
        data.call(sakura, salao, phone, at(day, 20, 40), at(day, 20, 40).plusSeconds(30), ana);
        jdbc.update("""
                INSERT INTO call (id, establishment_id, table_id, session_id, type, status, created_at, canceled_at)
                VALUES (?, ?, ?, ?, 'WAITER', 'CANCELED', ?, ?)
                """, UUID.randomUUID(), sakura.establishmentId(), salao.table(), phone, Timestamp.from(at(day, 20, 50)),
                Timestamp.from(at(day, 20, 55)));
        jdbc.update("""
                INSERT INTO call (id, establishment_id, table_id, session_id, type, payment_method, status, created_at)
                VALUES (?, ?, ?, ?, 'BILL', 'PIX', 'PENDING', ?)
                """, UUID.randomUUID(), sakura.establishmentId(), salao.table(), phone, Timestamp.from(at(day, 21, 20)));

        UUID device = UUID.randomUUID();
        jdbc.update("INSERT INTO device (id, establishment_id, name, type, created_at) VALUES (?, ?, 'Celular', 'WAITER', now())",
                device, sakura.establishmentId());
        jdbc.update("""
                INSERT INTO staff_shift (id, establishment_id, user_id, device_id, family_id, started_at, ended_at, end_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'LOGOUT')
                """, UUID.randomUUID(), sakura.establishmentId(), carlos, device, UUID.randomUUID(),
                Timestamp.from(at(day, 19, 0)), Timestamp.from(at(day, 23, 0)));
        for (int i = 0; i < 3; i++) {
            jdbc.update("""
                    INSERT INTO duplicate_warning (id, establishment_id, tab_id, product_id, warned_by, warned_at, confirmed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), sakura.establishmentId(), dinner, prato, carlos,
                    Timestamp.from(at(day, 20, 10 + i)), i == 0 ? Timestamp.from(at(day, 20, 11)) : null);
        }
    }

    private String team() {
        return "/api/v1/reports/team?from=%s&to=%s".formatted(first, day);
    }

    @Test
    void theProAndTheStaffCannotSeeIt() {
        assertThat(data.get(sakura, team())).hasStatus(HttpStatus.FORBIDDEN);
        data.onPlan(sakura, "business");
        assertThat(data.get(data.token(sakura, Role.STAFF), team())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(data.get(data.token(sakura, Role.MANAGER), team())).hasStatusOk();
    }

    @Test
    void theCallsAndTheTimes() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, team());

        assertThat(json(result, "$.current.calls")).isEqualTo("5");
        assertThat(json(result, "$.current.responseSeconds")).isEqualTo("70");
        assertThat(json(result, "$.current.doneSeconds")).isEqualTo("70");
        assertThat(json(result, "$.current.billToCloseSeconds")).isEqualTo("600");
        assertThat(json(result, "$.compared.calls")).isEqualTo("0");
        assertThat(json(result, "$.responseByHour[0].hour")).isEqualTo("20");
        assertThat(json(result, "$.byType[0].type")).isEqualTo("WAITER");
        assertThat(json(result, "$.byType[0].calls")).isEqualTo("4");
        assertThat(json(result, "$.byType[0].canceled")).isEqualTo("1");
    }

    @Test
    void theWaitersTable() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, team());

        List<String> names = read(result, "$.waiters[*].name");
        assertThat(names).startsWith("Carlos", "Ana");
        assertThat(json(result, "$.waiters[0].calls")).isEqualTo("2");
        assertThat(json(result, "$.waiters[0].responseSeconds")).isEqualTo("90");
        assertThat(json(result, "$.waiters[0].delivered")).isEqualTo("2");
        assertThat(json(result, "$.waiters[0].closedTabs")).isEqualTo("1");
        assertThat(json(result, "$.waiters[0].revenueCents")).isEqualTo("5000");
        assertThat(json(result, "$.waiters[0].tipsCents")).isEqualTo("500");
        // O dono lançou os pedidos das três comandas e fechou as outras duas.
        assertThat(names).contains("Marina");
    }

    @Test
    void tablesWarningsAndShifts() throws Exception {
        data.onPlan(sakura, "business");

        MvcTestResult result = data.get(sakura, team());

        assertThat(json(result, "$.topTables[0].label")).isEqualTo("Sa1");
        assertThat(json(result, "$.topTables[0].calls")).isEqualTo("5");
        assertThat(json(result, "$.topTables[0].tabs")).isEqualTo("3");
        assertThat(json(result, "$.warningsShown")).isEqualTo("3");
        assertThat(json(result, "$.warningsConfirmed")).isEqualTo("1");
        assertThat(json(result, "$.busiestShifts[0].hour")).isEqualTo("20");
        assertThat(json(result, "$.busiestShifts[0].calls")).isEqualTo("4");
        assertThat(json(result, "$.busiestShifts[0].waiters")).isEqualTo("1");
    }

    @Test
    void anotherRestaurantNeverShowsUp() throws Exception {
        data.onPlan(sakura, "business");
        Owner other = data.newRestaurant();
        data.onPlan(other, "business");
        Place theirs = data.place(other, "Varanda");
        UUID session = data.session(other, theirs, at(day, 20, 0));
        data.call(other, theirs, session, at(day, 20, 0), at(day, 20, 5), data.waiter(other, "Bruno"));

        MvcTestResult mine = data.get(sakura, team());
        MvcTestResult theirsReport = data.get(other, team());

        assertThat(read(mine, "$.waiters[*].name").toString()).doesNotContain("Bruno");
        assertThat(json(mine, "$.current.calls")).isEqualTo("5");
        assertThat(json(theirsReport, "$.current.calls")).isEqualTo("1");
        assertThat(json(theirsReport, "$.warningsShown")).isEqualTo("0");
        assertThat(data.get(sakura, team() + "&sectorId=" + theirs.sector())).hasStatus(HttpStatus.NOT_FOUND);
    }
}
