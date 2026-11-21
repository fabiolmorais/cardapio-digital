package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.domain.report.model.DayComparison;
import br.com.cardapio.domain.report.model.HourAverage;
import br.com.cardapio.domain.report.model.ItemSales;
import br.com.cardapio.domain.report.model.PaymentTotal;
import br.com.cardapio.domain.report.model.ReportPeriod;
import br.com.cardapio.domain.report.model.ReportTotals;
import br.com.cardapio.domain.report.model.SeriesPoint;
import br.com.cardapio.domain.report.model.WeekdayHourAverage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** As peças das respostas dos relatórios: dinheiro sempre em centavos. */
final class ReportDtos {

    private ReportDtos() {
    }

    enum Level { NONE, BASIC, FULL }

    @Schema(name = "ReportRange", description = "Dias do restaurante, do primeiro ao último (inclusive)")
    record Range(@Schema(requiredMode = REQUIRED) LocalDate from, @Schema(requiredMode = REQUIRED) LocalDate to) {
        static Range of(ReportPeriod period) {
            return new Range(period.from(), period.to());
        }
    }

    @Schema(name = "ReportTotals", description = "As comandas fechadas do período")
    record Totals(@Schema(requiredMode = REQUIRED, description = "Faturamento, sem a taxa de serviço") long revenueCents,
                  @Schema(requiredMode = REQUIRED) int tabs,
                  @Schema(requiredMode = REQUIRED) long averageTicketCents) {
        static Totals of(ReportTotals totals) {
            return new Totals(totals.revenue().cents(), totals.tabs(), totals.averageTicket().cents());
        }
    }

    @Schema(name = "ReportDay", description = "Um dia do período ao lado do dia correspondente da comparação")
    record Day(@Schema(requiredMode = REQUIRED) LocalDate day, @Schema(requiredMode = REQUIRED) long revenueCents,
               LocalDate comparedDay, @Schema(requiredMode = REQUIRED) long comparedRevenueCents) {
        static Day of(DayComparison day) {
            return new Day(day.day(), day.revenue().cents(), day.comparedDay(), day.comparedRevenue().cents());
        }
    }

    enum Method { PIX, CARD, CASH }

    @Schema(name = "ReportPayment", description = "O que entrou por forma de pagamento, com a taxa de serviço")
    record Payment(@Schema(requiredMode = REQUIRED) Method method, @Schema(requiredMode = REQUIRED) long amountCents) {
        static Payment of(PaymentTotal payment) {
            return new Payment(Method.valueOf(payment.method().name()), payment.amount().cents());
        }
    }

    @Schema(name = "ReportHeatCell", description = "Faturamento médio de um dia da semana numa hora")
    record HeatCell(@Schema(requiredMode = REQUIRED, description = "1 = segunda … 7 = domingo") int weekday,
                    @Schema(requiredMode = REQUIRED, description = "0 a 23") int hour,
                    @Schema(requiredMode = REQUIRED) long averageCents) {
        static HeatCell of(WeekdayHourAverage cell) {
            return new HeatCell(cell.isoWeekday(), cell.hour(), cell.average().cents());
        }
    }

    @Schema(name = "ReportItem", description = "Um produto vendido no período (sem os cancelados)")
    record Item(@Schema(requiredMode = REQUIRED) UUID productId, @Schema(requiredMode = REQUIRED) String name,
                @Schema(requiredMode = REQUIRED) int quantity, @Schema(requiredMode = REQUIRED) long revenueCents) {
        static Item of(ItemSales item) {
            return new Item(item.productId().value(), item.name(), item.quantity(), item.revenue().cents());
        }
    }

    @Schema(name = "ReportSeriesPoint", description = "Um dia, uma semana ou um mês do período")
    record Point(@Schema(requiredMode = REQUIRED) LocalDate start, @Schema(requiredMode = REQUIRED) LocalDate end,
                 @Schema(requiredMode = REQUIRED) long revenueCents, @Schema(requiredMode = REQUIRED) int tabs) {
        static Point of(SeriesPoint point) {
            return new Point(point.start(), point.end(), point.revenue().cents(), point.tabs());
        }
    }

    @Schema(name = "ReportHour", description = "Uma hora do dia, em média por dia do período")
    record Hour(@Schema(requiredMode = REQUIRED) int hour, @Schema(requiredMode = REQUIRED) long revenuePerDayCents,
                @Schema(requiredMode = REQUIRED) BigDecimal tabsPerDay) {
        static Hour of(HourAverage hour) {
            return new Hour(hour.hour(), hour.revenuePerDay().cents(), hour.tabsPerDay());
        }
    }

    @Schema(name = "ReportBestDay")
    record BestDay(@Schema(requiredMode = REQUIRED) LocalDate day, @Schema(requiredMode = REQUIRED) long revenueCents) {
    }
}
