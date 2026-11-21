package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.report.GetOverviewReportUseCase;
import br.com.cardapio.application.port.in.report.GetReportContextUseCase;
import br.com.cardapio.application.port.in.report.GetSalesReportUseCase;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Os relatórios (RF24, ADR 0013): só o dono e o gerente. */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class ReportController {

    private final GetReportContextUseCase context;
    private final GetOverviewReportUseCase overview;
    private final GetSalesReportUseCase sales;

    ReportController(GetReportContextUseCase context, GetOverviewReportUseCase overview, GetSalesReportUseCase sales) {
        this.context = context;
        this.overview = overview;
        this.sales = sales;
    }

    @GetMapping("/context")
    @Operation(operationId = "getReportContext",
            summary = "O plano, o fuso, o histórico liberado e desde quando há dados")
    @ApiResponse(responseCode = "200", description = "O contexto dos relatórios (também no Essencial)")
    ReportContextResponse context(@AuthenticationPrincipal Jwt jwt) {
        return ReportContextResponse.from(context.get(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @GetMapping("/overview")
    @Operation(operationId = "getOverviewReport", summary = "A Visão geral do período (blocos básicos: Pro em diante)")
    @ApiResponse(responseCode = "200", description = "Os números do período e da comparação")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor não encontrado", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OverviewReportResponse overview(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(required = false) UUID sectorId) {
        return OverviewReportResponse.from(overview.get(query(jwt, from, to, sectorId)));
    }

    @GetMapping("/sales")
    @Operation(operationId = "getSalesReport", summary = "Vendas do período (blocos básicos: Pro em diante)")
    @ApiResponse(responseCode = "200", description = "O faturamento no tempo e por hora")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor não encontrado", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    SalesReportResponse sales(@AuthenticationPrincipal Jwt jwt,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                              @RequestParam(required = false) UUID sectorId,
                              @RequestParam(defaultValue = "DAY") SalesReportResponse.Grouping groupBy) {
        return SalesReportResponse.from(sales.get(query(jwt, from, to, sectorId),
                SeriesGrouping.valueOf(groupBy.name())));
    }

    private static ReportQuery query(Jwt jwt, LocalDate from, LocalDate to, UUID sectorId) {
        return new ReportQuery(AuthenticatedUser.from(jwt).establishmentId(), from, to,
                sectorId == null ? null : new SectorId(sectorId));
    }
}
