package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.application.port.in.report.TableReport;
import br.com.cardapio.domain.report.model.ForgottenTab;
import br.com.cardapio.domain.report.model.HourShare;
import br.com.cardapio.domain.report.model.OccupancyPeak;
import br.com.cardapio.domain.report.model.StayTotals;
import br.com.cardapio.domain.report.model.TableUsage;
import br.com.cardapio.domain.shared.model.Money;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TableReport", description = "Mesas e ocupação (Business)")
record TableReportResponse(
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED) Range comparison,
        @Schema(requiredMode = REQUIRED) Stays current,
        @Schema(requiredMode = REQUIRED, description = "A comparação; se o período termina hoje, até a mesma hora")
        Stays compared,
        @Schema(requiredMode = REQUIRED, description = "As mesas ativas, por setor, para o mapa do salão")
        List<Table> tables,
        @Schema(requiredMode = REQUIRED, description = "A ocupação média por hora do dia, nos dias com movimento")
        List<Hour> occupancyByHour,
        @Schema(description = "A hora da semana mais cheia; null sem comandas") Peak peak,
        @Schema(requiredMode = REQUIRED, description = "Comandas abertas no período há mais de 6 horas")
        int forgottenCount,
        @Schema(requiredMode = REQUIRED, description = "As mais recentes") List<Forgotten> forgotten,
        @Schema(requiredMode = REQUIRED) Moves moves) {

    private static Long seconds(Optional<Duration> d) {
        return d.map(Duration::toSeconds).orElse(null);
    }

    @Schema(name = "ReportStays", description = "As comandas fechadas e o tempo de mesa")
    record Stays(@Schema(requiredMode = REQUIRED) int tabs,
                 @Schema(description = "Da abertura ao fechamento, em média") Long staySeconds,
                 @Schema(description = "Só as comandas com rodízio") Long rodizioStaySeconds,
                 @Schema(description = "Só as sem rodízio") Long aLaCarteStaySeconds,
                 @Schema(description = "Comandas por mesa por dia; null sem mesas") Double turnover,
                 @Schema(description = "Faturamento por hora de mesa ocupada; null sem comandas")
                 Long perOccupiedHourCents) {
        static Stays of(StayTotals s, Optional<Double> turnover, Optional<Money> perHour) {
            return new Stays(s.tabs(), seconds(s.stay()), seconds(s.rodizioStay()), seconds(s.aLaCarteStay()),
                    turnover.orElse(null), perHour.map(Money::cents).orElse(null));
        }
    }

    @Schema(name = "ReportTableUsage", description = "Uma mesa: comandas fechadas e faturamento")
    record Table(@Schema(requiredMode = REQUIRED) String label, @Schema(requiredMode = REQUIRED) String sector,
                 @Schema(requiredMode = REQUIRED) int tabs, @Schema(requiredMode = REQUIRED) long revenueCents,
                 @Schema(description = "Tíquete médio; null sem comandas") Long ticketCents) {
        static Table of(TableUsage t) {
            return new Table(t.label(), t.sector(), t.tabs(), t.revenue().cents(),
                    t.ticket().map(Money::cents).orElse(null));
        }
    }

    @Schema(name = "ReportOccupancyHour", description = "A fração das mesas ocupadas numa hora (0 a 1)")
    record Hour(@Schema(requiredMode = REQUIRED) int hour, @Schema(requiredMode = REQUIRED) double share) {
        static Hour of(HourShare h) {
            return new Hour(h.hour(), h.share());
        }
    }

    @Schema(name = "ReportOccupancyPeak", description = "O dia da semana e a hora mais cheios")
    record Peak(@Schema(requiredMode = REQUIRED) DayOfWeek weekday, @Schema(requiredMode = REQUIRED) int hour,
                @Schema(requiredMode = REQUIRED) double share) {
        static Peak of(OccupancyPeak p) {
            return new Peak(p.weekday(), p.hour(), p.share());
        }
    }

    @Schema(name = "ReportForgottenTab", description = "Uma comanda aberta tempo demais")
    record Forgotten(@Schema(requiredMode = REQUIRED) UUID tabId, @Schema(requiredMode = REQUIRED) String label,
                     @Schema(requiredMode = REQUIRED) Instant openedAt,
                     @Schema(description = "null: ainda aberta") Instant closedAt) {
        static Forgotten of(ForgottenTab f) {
            return new Forgotten(f.tabId().value(), f.label(), f.openedAt(), f.closedAt().orElse(null));
        }
    }

    @Schema(name = "ReportTableMoves", description = "Mudanças de mesa e junções de comandas")
    record Moves(@Schema(requiredMode = REQUIRED) int moves, @Schema(requiredMode = REQUIRED) int merges,
                 @Schema(description = "A mesa de onde o grupo mais saiu; null sem mudanças") String topFrom,
                 @Schema(requiredMode = REQUIRED) int topFromMoves) {
    }

    static TableReportResponse from(TableReport r) {
        var moves = r.moves();
        return new TableReportResponse(Range.of(r.period()), Range.of(r.comparison()),
                Stays.of(r.current(), r.turnover(), r.perOccupiedHour()),
                Stays.of(r.compared(), r.comparedTurnover(), r.comparedPerOccupiedHour()),
                r.tables().stream().map(Table::of).toList(),
                r.occupancy().byHour().stream().map(Hour::of).toList(),
                r.occupancy().peak().map(Peak::of).orElse(null),
                r.forgotten().count(), r.forgotten().latest().stream().map(Forgotten::of).toList(),
                new Moves(moves.moves(), moves.merges(), moves.topFrom().orElse(null), moves.topFromMoves()));
    }
}
