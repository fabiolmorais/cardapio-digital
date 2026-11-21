package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.in.report.ReportContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "ReportContext", description = "O que a tela dos relatórios precisa saber antes de pedir os números")
record ReportContextResponse(
        @Schema(requiredMode = REQUIRED, description = "Nenhum (Essencial), básicos (Pro) ou completos (Business)")
        Level level,
        @Schema(requiredMode = REQUIRED, description = "O plano exporta os relatórios")
        boolean canExport,
        @Schema(requiredMode = REQUIRED, description = "O fuso do restaurante: o dia dos relatórios", example = "America/Sao_Paulo")
        String timeZone,
        @Schema(requiredMode = REQUIRED, description = "Hoje, no fuso do restaurante")
        LocalDate today,
        @Schema(description = "O primeiro dia que o plano deixa consultar; ausente quando não há limite")
        LocalDate earliestDay,
        @Schema(description = "O dia da primeira comanda fechada; ausente enquanto não há dados")
        LocalDate firstDataDay,
        @Schema(requiredMode = REQUIRED, description = "O maior período de uma vez, em dias")
        int maxPeriodDays) {

    enum Level { NONE, BASIC, FULL }

    static ReportContextResponse from(ReportContext context) {
        return new ReportContextResponse(Level.valueOf(context.level().name()), context.canExport(),
                context.timeZone().getId(), context.today(), context.earliestDay(), context.firstDataDay(),
                context.maxPeriodDays());
    }
}
