package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Totals;
import br.com.cardapio.application.port.in.report.UnitComparison;
import br.com.cardapio.domain.report.model.NetworkTotals;
import br.com.cardapio.domain.report.model.TimeAverage;
import br.com.cardapio.domain.report.model.UnitPerformance;
import br.com.cardapio.domain.report.model.UnitPoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "UnitComparison", description = "A comparação entre unidades (REL-80, Enterprise)")
record UnitComparisonResponse(
        @Schema(requiredMode = REQUIRED, description = "O período no fuso da unidade de agora") Range period,
        @Schema(requiredMode = REQUIRED) Range comparison,
        @Schema(requiredMode = REQUIRED, description = "Por dia até 14 dias, por semana até 6 meses, por mês acima")
        SalesReportResponse.Grouping grouping,
        @Schema(requiredMode = REQUIRED, description = "Da que mais faturou para a que menos faturou") List<Unit> units,
        @Schema(requiredMode = REQUIRED) Figures network) {

    @Schema(name = "UnitFigures", description = "Os números de uma unidade, ou da rede inteira")
    record Figures(@Schema(requiredMode = REQUIRED) Totals current,
                   @Schema(requiredMode = REQUIRED, description = "O período anterior") Totals compared,
                   @Schema(description = "Preparo médio, em segundos; ausente sem preparos") Long prepSeconds,
                   @Schema(description = "Resposta média ao chamado, em segundos; ausente sem chamados aceitos")
                   Long callResponseSeconds,
                   @Schema(description = "A nota média, com uma casa; ausente com menos de 5 avaliações")
                   Double ratingAverage,
                   @Schema(requiredMode = REQUIRED) int ratings) {

        static Figures of(UnitPerformance u) {
            return new Figures(Totals.of(u.current()), Totals.of(u.compared()), seconds(u.prep()),
                    seconds(u.callResponse()), u.ratingAverage(), u.ratings().count());
        }

        static Figures of(NetworkTotals n) {
            return new Figures(Totals.of(n.current()), Totals.of(n.compared()), seconds(n.prep()),
                    seconds(n.callResponse()), n.ratingAverage(), n.ratings().count());
        }

        private static Long seconds(TimeAverage average) {
            return average.average().map(Duration::toSeconds).orElse(null);
        }
    }

    @Schema(name = "UnitComparisonUnit")
    record Unit(@Schema(requiredMode = REQUIRED) UUID id,
                @Schema(requiredMode = REQUIRED) String name,
                @Schema(requiredMode = REQUIRED) Figures figures,
                @Schema(requiredMode = REQUIRED) List<Point> series) {

        static Unit of(UnitPerformance u) {
            return new Unit(u.id().value(), u.name(), Figures.of(u), u.series().stream().map(Point::of).toList());
        }
    }

    @Schema(name = "UnitComparisonPoint", description = "Um ponto da evolução, com as médias por dia")
    record Point(@Schema(requiredMode = REQUIRED) LocalDate start,
                 @Schema(requiredMode = REQUIRED) LocalDate end,
                 @Schema(requiredMode = REQUIRED) long revenuePerDayCents,
                 @Schema(requiredMode = REQUIRED) BigDecimal tabsPerDay) {

        static Point of(UnitPoint p) {
            return new Point(p.start(), p.end(), p.revenuePerDay().cents(), p.tabsPerDay());
        }
    }

    static UnitComparisonResponse from(UnitComparison c) {
        return new UnitComparisonResponse(Range.of(c.period()), Range.of(c.comparison()),
                SalesReportResponse.Grouping.valueOf(c.grouping().name()),
                c.units().stream().map(Unit::of).toList(), Figures.of(c.network()));
    }
}
