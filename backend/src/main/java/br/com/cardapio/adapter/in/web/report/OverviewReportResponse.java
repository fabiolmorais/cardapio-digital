package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Day;
import br.com.cardapio.adapter.in.web.report.ReportDtos.HeatCell;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Item;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Level;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Payment;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.adapter.in.web.report.ReportDtos.Totals;
import br.com.cardapio.application.port.in.report.OverviewReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "OverviewReport", description = "A Visão geral, com os blocos básicos")
record OverviewReportResponse(
        @Schema(requiredMode = REQUIRED) Level level,
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED, description = "Mesmo tamanho antes; um dia só: o mesmo dia da semana anterior")
        Range comparison,
        @Schema(requiredMode = REQUIRED) Totals current,
        @Schema(requiredMode = REQUIRED, description = "A comparação; se o período termina hoje, até a mesma hora")
        Totals compared,
        @Schema(requiredMode = REQUIRED) List<Day> byDay,
        @Schema(requiredMode = REQUIRED) List<Payment> payments,
        @Schema(requiredMode = REQUIRED, description = "Só as células com venda") List<HeatCell> heatmap,
        @Schema(requiredMode = REQUIRED) List<Item> topItems,
        @Schema(description = "Os blocos do Business; null no Pro (o dado não sai do servidor)")
        OverviewFullDtos.Full full) {

    static OverviewReportResponse from(OverviewReport r) {
        return new OverviewReportResponse(Level.valueOf(r.level().name()), Range.of(r.period()),
                Range.of(r.comparison()), Totals.of(r.current()), Totals.of(r.compared()),
                r.byDay().stream().map(Day::of).toList(), r.payments().stream().map(Payment::of).toList(),
                r.heatmap().stream().map(HeatCell::of).toList(), r.topItems().stream().map(Item::of).toList(),
                r.full() == null ? null : OverviewFullDtos.Full.of(r.full()));
    }
}
