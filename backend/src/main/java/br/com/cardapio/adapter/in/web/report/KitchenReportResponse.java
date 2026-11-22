package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Level;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.adapter.in.web.report.ReportDtos.SlowItem;
import br.com.cardapio.application.port.in.report.KitchenReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "KitchenReport", description = "Cozinha e tempos, com os blocos básicos")
record KitchenReportResponse(
        @Schema(requiredMode = REQUIRED) Level level,
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED, description = "Mesmo tamanho antes; um dia só: o mesmo dia da semana anterior")
        Range comparison,
        @Schema(description = "Preparo médio, da chegada à cozinha até pronto (sem itens prontos, null)")
        Long averagePrepSeconds,
        @Schema(description = "O da comparação; se o período termina hoje, até a mesma hora")
        Long comparedAveragePrepSeconds,
        @Schema(requiredMode = REQUIRED, description = "Itens que ficaram prontos no período") int preparations,
        @Schema(requiredMode = REQUIRED, description = "Os que mais demoram (5 preparos ou mais), pelo 9 em 10")
        List<SlowItem> slowest,
        @Schema(description = "Os blocos do Business; null no Pro (o dado não sai do servidor)") KitchenFullDtos.Full full) {

    static KitchenReportResponse from(KitchenReport r) {
        return new KitchenReportResponse(Level.valueOf(r.level().name()), Range.of(r.period()),
                Range.of(r.comparison()), r.averagePrep().map(Duration::toSeconds).orElse(null),
                r.comparedAveragePrep().map(Duration::toSeconds).orElse(null), r.preparations(),
                r.slowest().stream().map(SlowItem::of).toList(),
                r.full() == null ? null : KitchenFullDtos.Full.of(r.full()));
    }
}
