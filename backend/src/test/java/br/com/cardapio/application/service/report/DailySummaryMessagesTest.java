package br.com.cardapio.application.service.report;

import br.com.cardapio.application.port.in.report.DailySummary;
import br.com.cardapio.domain.account.model.Role;
import br.com.cardapio.domain.call.model.PaymentMethod;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.domain.report.model.HourlyRevenue;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SlowAttention;
import br.com.cardapio.domain.report.model.SummaryWindow;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.domain.shared.model.Money;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DailySummaryMessagesTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Email MARINA = new Email("marina@sakura.test");

    /** Sexta, 13/11/2026: R$ 9.870 em 84 comandas, contra R$ 9.020 em 78 na sexta passada. */
    private static DailySummary friday() {
        return new DailySummary("Sakura <Sushi>", new SummaryWindow(LocalDate.of(2026, 11, 13), 2, SAO_PAULO),
                new ReportTotals(Money.ofCents(987000), 84), new ReportTotals(Money.ofCents(902000), 78),
                Optional.of(Duration.ofMinutes(12)), Optional.of(Duration.ofMinutes(13)),
                Optional.of(new ItemSales(new ProductId(UUID.randomUUID()), "Combinado 30 peças", 23,
                        Money.ofCents(230000))),
                Optional.of(new HourlyRevenue(5, 21, Money.ofCents(234000), 18)),
                Optional.of(new SlowAttention("Guioza", Duration.ofMinutes(26), Duration.ofMinutes(14))),
                List.of(new PaymentTotal(PaymentMethod.PIX, Money.ofCents(464000)),
                        new PaymentTotal(PaymentMethod.CARD, Money.ofCents(425000))), Optional.empty());
    }

    private static DailySummary withGoal(GoalProgress goal) {
        DailySummary f = friday();
        return new DailySummary(f.establishmentName(), f.window(), f.current(), f.compared(), f.averagePrep(),
                f.comparedAveragePrep(), f.champion(), f.peak(), f.attention(), f.payments(), Optional.of(goal));
    }

    @Test
    void theGoalOfTheMonthShowsWhatIsMissingPerDay() {
        GoalProgress goal = new GoalProgress(YearMonth.of(2026, 11), Money.ofCents(18000000), Money.ofCents(12430000),
                17, Optional.of(Money.ofCents(326000)), Optional.of(Money.ofCents(956200)));

        var email = DailySummaryMessages.email(withGoal(goal), MARINA, Role.OWNER, "https://ohchefe.test");

        assertThat(email.text()).contains("Meta de novembro: R$ 124.300,00 de R$ 180.000,00 (69%). Faltam 17 dias: "
                + "R$ 3.260,00 por dia batem a meta (a média está em R$ 9.562,00).");
        assertThat(email.html()).contains("Meta de novembro").contains("width:69%");
    }

    @Test
    void aReachedGoalSaysSo() {
        GoalProgress goal = new GoalProgress(YearMonth.of(2026, 11), Money.ofCents(10000000), Money.ofCents(10500000),
                17, Optional.empty(), Optional.empty());

        var email = DailySummaryMessages.email(withGoal(goal), MARINA, Role.OWNER, "https://ohchefe.test");

        assertThat(email.text()).contains("Meta de novembro: batida! R$ 105.000,00 de R$ 100.000,00 (105%).");
        assertThat(email.html()).contains("width:100%");
    }

    @Test
    void theEmailHasTheNumbersOfTheDayAgainstTheSameWeekdayBefore() {
        var email = DailySummaryMessages.email(friday(), MARINA, Role.OWNER, "https://ohchefe.test");

        assertThat(email.to()).isEqualTo(MARINA);
        assertThat(email.subject()).isEqualTo("Resumo de sexta, 13 de novembro · R$ 9.870,00");
        assertThat(email.text())
                .contains("Seu dia: sexta, 13 de novembro")
                .contains("Faturamento: R$ 9.870,00 (▲ 9,4% vs. sexta passada)")
                .contains("Comandas: 84 (▲ 6)")
                .contains("Tíquete médio: R$ 117,50 (▲ 1,6%)")
                .contains("Preparo médio: 12 min (▼ 1 min, mais rápido)")
                .contains("Campeão do dia: Combinado 30 peças, 23 vendidos.")
                .contains("Pico: 21h, com R$ 2.340,00 em 18 comandas que chegaram nessa hora.")
                .contains("Atenção: Guioza levou 26 min em média (normal: 14 min).")
                .contains("Pagamentos: Pix R$ 4.640,00 · Cartão R$ 4.250,00 (com a taxa de serviço).")
                .contains("https://ohchefe.test/admin/relatorios?de=2026-11-13&ate=2026-11-13")
                .contains("porque é dono do Sakura <Sushi> no OhChefe")
                .contains("https://ohchefe.test/admin/relatorios/resumo");
    }

    @Test
    void theHtmlEscapesWhatComesFromTheRegistration() {
        var email = DailySummaryMessages.email(friday(), MARINA, Role.MANAGER, "https://ohchefe.test");

        assertThat(email.html()).contains("Sakura &lt;Sushi&gt;").doesNotContain("Sakura <Sushi>")
                .contains("porque é gerente do")
                .contains("href=\"https://ohchefe.test/admin/relatorios?de=2026-11-13&amp;ate=2026-11-13\"");
    }

    @Test
    void theWeekendIsMasculineAndAnEmptyWeekBeforeHasNoVariation() {
        DailySummary f = friday();
        var saturday = new DailySummary(f.establishmentName(), new SummaryWindow(LocalDate.of(2026, 11, 14), 2,
                SAO_PAULO), new ReportTotals(Money.ofCents(10000), 1), ReportTotals.EMPTY, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), Optional.empty());

        var email = DailySummaryMessages.email(saturday, MARINA, Role.OWNER, "https://ohchefe.test");
        var push = DailySummaryMessages.push(saturday);

        assertThat(email.text()).contains("Faturamento: R$ 100,00 (sem movimento no sábado passado)")
                .contains("Comandas: 1 (sem movimento no sábado passado)")
                .doesNotContain("Preparo médio").doesNotContain("Campeão").doesNotContain("Meta de");
        assertThat(push.title()).isEqualTo("Resumo de sábado");
        assertThat(push.body()).isEqualTo("R$ 100,00 em 1 comanda. Toque para ver.");
    }

    @Test
    void thePushIsShortAndOpensTheReportsOfTheDay() {
        var push = DailySummaryMessages.push(friday());

        assertThat(push.title()).isEqualTo("Resumo de sexta");
        assertThat(push.body()).isEqualTo("R$ 9.870,00 em 84 comandas, 9% acima da sexta passada. Toque para ver.");
        assertThat(push.url()).isEqualTo("/admin/relatorios?de=2026-11-13&ate=2026-11-13");
        assertThat(push.tag()).isEqualTo(DailySummaryMessages.PUSH_TAG);
    }

    @Test
    void timesAreWrittenLikeTheKitchenScreen() {
        assertThat(DailySummaryMessages.duration(Duration.ofSeconds(45))).isEqualTo("45 s");
        assertThat(DailySummaryMessages.duration(Duration.ofSeconds(220))).isEqualTo("3 min 40 s");
        assertThat(DailySummaryMessages.duration(Duration.ofSeconds(240))).isEqualTo("4 min");
        assertThat(DailySummaryMessages.duration(Duration.ofSeconds(731))).isEqualTo("12 min");
        assertThat(DailySummaryMessages.duration(Duration.ofMinutes(65))).isEqualTo("1 h 05 min");
        assertThat(DailySummaryMessages.money(5)).isEqualTo("R$ 0,05");
    }
}
