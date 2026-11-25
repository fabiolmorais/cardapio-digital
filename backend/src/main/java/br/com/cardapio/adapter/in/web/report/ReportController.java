package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.report.GetKitchenReportUseCase;
import br.com.cardapio.application.port.in.report.GetMenuReportUseCase;
import br.com.cardapio.application.port.in.report.GetOverviewReportUseCase;
import br.com.cardapio.application.port.in.report.GetReportContextUseCase;
import br.com.cardapio.application.port.in.report.GetSalesReportUseCase;
import br.com.cardapio.application.port.in.report.GetPhoneRodizioReportUseCase;
import br.com.cardapio.application.port.in.report.GetTableReportUseCase;
import br.com.cardapio.application.port.in.report.GetTeamReportUseCase;
import br.com.cardapio.application.port.in.report.KitchenReportQuery;
import br.com.cardapio.application.port.in.report.MenuReportQuery;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.report.model.ProductSort;
import br.com.cardapio.domain.report.model.SeriesGrouping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Os relatórios (RF24, ADR 0013): só o dono e o gerente. */
@RestController
@Validated
@RequestMapping("/api/v1/reports")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class ReportController {

    private final GetReportContextUseCase context;
    private final GetOverviewReportUseCase overview;
    private final GetSalesReportUseCase sales;
    private final GetMenuReportUseCase menu;
    private final GetKitchenReportUseCase kitchen;
    private final GetTeamReportUseCase team;
    private final GetTableReportUseCase tables;
    private final GetPhoneRodizioReportUseCase phoneRodizio;

    ReportController(GetReportContextUseCase context, GetOverviewReportUseCase overview, GetSalesReportUseCase sales,
                     GetMenuReportUseCase menu, GetKitchenReportUseCase kitchen, GetTeamReportUseCase team,
                     GetTableReportUseCase tables, GetPhoneRodizioReportUseCase phoneRodizio) {
        this.context = context;
        this.overview = overview;
        this.sales = sales;
        this.menu = menu;
        this.kitchen = kitchen;
        this.team = team;
        this.tables = tables;
        this.phoneRodizio = phoneRodizio;
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

    @GetMapping("/menu")
    @Operation(operationId = "getMenuReport", summary = "O Cardápio do período (blocos básicos: Pro em diante)")
    @ApiResponse(responseCode = "200", description = "Por categoria, os que vendem pouco e os totais")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor ou categoria não encontrados", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    MenuReportResponse menu(@AuthenticationPrincipal Jwt jwt,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) UUID sectorId,
                            @RequestParam(required = false) UUID categoryId) {
        return MenuReportResponse.from(menu.get(menuQuery(jwt, from, to, sectorId, categoryId)));
    }

    @GetMapping("/menu/products")
    @Operation(operationId = "listMenuReportProducts",
            summary = "Os produtos vendidos no período, por faturamento, quantidade ou crescimento (paginado)")
    @ApiResponse(responseCode = "200", description = "Uma página dos produtos")
    @ApiResponse(responseCode = "400", description = "Datas ou página inválidas", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios (ou o crescimento, fora do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor ou categoria não encontrados", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<MenuFullDtos.Row> menuProducts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID sectorId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "REVENUE") ProductOrder sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(menu.products(menuQuery(jwt, from, to, sectorId, categoryId),
                ProductSort.valueOf(sort.name()), new PageQuery(page, size)), MenuFullDtos.Row::of);
    }

    @GetMapping("/kitchen")
    @Operation(operationId = "getKitchenReport", summary = "Cozinha e tempos do período (blocos básicos: Pro em diante)")
    @ApiResponse(responseCode = "200", description = "O preparo médio e os itens que mais demoram")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor ou categoria não encontrados", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    KitchenReportResponse kitchen(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) UUID sectorId,
                                  @RequestParam(required = false) UUID categoryId) {
        return KitchenReportResponse.from(kitchen.get(new KitchenReportQuery(query(jwt, from, to, sectorId),
                categoryId == null ? null : new MenuCategoryId(categoryId))));
    }

    @GetMapping("/team")
    @Operation(operationId = "getTeamReport", summary = "Atendimento e equipe do período (Business)")
    @ApiResponse(responseCode = "200", description = "Os chamados, os garçons e a trava de repetidos")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem este relatório (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor não encontrado", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TeamReportResponse team(@AuthenticationPrincipal Jwt jwt,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) UUID sectorId) {
        return TeamReportResponse.from(team.get(query(jwt, from, to, sectorId)));
    }

    @GetMapping("/team/comments")
    @Operation(operationId = "listTeamRatingComments",
            summary = "Os comentários das avaliações do período, do mais novo (Business, paginado)")
    @ApiResponse(responseCode = "200", description = "Uma página dos comentários")
    @ApiResponse(responseCode = "403", description = "O plano não tem este relatório (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor não encontrado", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<TeamReportResponse.Comment> teamComments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID sectorId,
            @RequestParam(defaultValue = "false") boolean onlyLow,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(team.comments(query(jwt, from, to, sectorId), onlyLow, new PageQuery(page, size)),
                TeamReportResponse.Comment::of);
    }

    @GetMapping("/tables")
    @Operation(operationId = "getTableReport", summary = "Mesas e ocupação do período (Business)")
    @ApiResponse(responseCode = "200", description = "A permanência, o giro, o mapa do salão e a ocupação")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem este relatório (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor não encontrado", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TableReportResponse tables(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) UUID sectorId) {
        return TableReportResponse.from(tables.get(query(jwt, from, to, sectorId)));
    }

    @GetMapping("/phone-and-rodizio")
    @Operation(operationId = "getPhoneRodizioReport", summary = "Celular e rodízio do período (Business)")
    @ApiResponse(responseCode = "200", description = "Celular x garçom, do QR ao pedido, presença e rodízio")
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem este relatório (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor não encontrado", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PhoneRodizioReportResponse phones(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) UUID sectorId) {
        return PhoneRodizioReportResponse.from(phoneRodizio.get(query(jwt, from, to, sectorId)));
    }

    /** A ordem da tabela de produtos (pelo crescimento: Business). */
    enum ProductOrder { REVENUE, QUANTITY, GROWTH }

    private static MenuReportQuery menuQuery(Jwt jwt, LocalDate from, LocalDate to, UUID sectorId, UUID categoryId) {
        return new MenuReportQuery(query(jwt, from, to, sectorId),
                categoryId == null ? null : new MenuCategoryId(categoryId));
    }

    private static ReportQuery query(Jwt jwt, LocalDate from, LocalDate to, UUID sectorId) {
        return new ReportQuery(AuthenticatedUser.from(jwt).establishmentId(), from, to,
                sectorId == null ? null : new SectorId(sectorId));
    }
}
