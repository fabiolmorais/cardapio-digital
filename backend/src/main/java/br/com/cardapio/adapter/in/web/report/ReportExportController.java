package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.export.CsvFormat;
import br.com.cardapio.adapter.in.web.report.export.PdfFormat;
import br.com.cardapio.adapter.in.web.report.export.ReportSheets;
import br.com.cardapio.adapter.in.web.report.export.Sheet;
import br.com.cardapio.adapter.in.web.report.export.XlsxFormat;
import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.report.AuthorizeReportExportUseCase;
import br.com.cardapio.application.port.in.report.GetKitchenReportUseCase;
import br.com.cardapio.application.port.in.report.GetMenuReportUseCase;
import br.com.cardapio.application.port.in.report.GetOverviewReportUseCase;
import br.com.cardapio.application.port.in.report.GetPhoneRodizioReportUseCase;
import br.com.cardapio.application.port.in.report.GetSalesReportUseCase;
import br.com.cardapio.application.port.in.report.GetTableReportUseCase;
import br.com.cardapio.application.port.in.report.GetTeamReportUseCase;
import br.com.cardapio.application.port.in.report.KitchenReportQuery;
import br.com.cardapio.application.port.in.report.MenuProduct;
import br.com.cardapio.application.port.in.report.MenuReportQuery;
import br.com.cardapio.application.port.in.report.ReportQuery;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A exportação dos relatórios (RF29) em CSV, Excel ou PDF: as mesmas tabelas da tela, com o mesmo
 * período, setor e filtros. Só o dono e o gerente, e só nos planos com exportação (Business); antes de
 * calcular, confere o plano.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class ReportExportController {

    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_TYPE = "application/pdf";

    /** O formato do arquivo: o tipo e a extensão. */
    enum ExportFormat {
        CSV(new MediaType("text", "csv", StandardCharsets.UTF_8), "csv"),
        XLSX(MediaType.parseMediaType(XLSX_TYPE), "xlsx"),
        PDF(MediaType.APPLICATION_PDF, "pdf");

        private final MediaType type;
        private final String extension;

        ExportFormat(MediaType type, String extension) {
            this.type = type;
            this.extension = extension;
        }
    }

    /** As respostas comuns das exportações, para a descrição da API. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "200", description = "A planilha: CSV (UTF-8, ponto e vírgula), Excel ou PDF",
            content = {
                    @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = XLSX_TYPE, schema = @Schema(type = "string", format = "binary")),
                    @Content(mediaType = PDF_TYPE, schema = @Schema(type = "string", format = "binary"))})
    @ApiResponse(responseCode = "400", description = "Datas inválidas ou período de mais de 12 meses", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não exporta relatórios (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Setor ou categoria não encontrados", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Período fora do histórico do plano ou no futuro", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @interface ExportResponses {
    }

    private final AuthorizeReportExportUseCase authorize;
    private final GetOverviewReportUseCase overview;
    private final GetSalesReportUseCase sales;
    private final GetMenuReportUseCase menu;
    private final GetKitchenReportUseCase kitchen;
    private final GetTeamReportUseCase team;
    private final GetTableReportUseCase tables;
    private final GetPhoneRodizioReportUseCase phoneRodizio;
    private final Clock clock;

    ReportExportController(AuthorizeReportExportUseCase authorize, GetOverviewReportUseCase overview,
                           GetSalesReportUseCase sales, GetMenuReportUseCase menu, GetKitchenReportUseCase kitchen,
                           GetTeamReportUseCase team, GetTableReportUseCase tables,
                           GetPhoneRodizioReportUseCase phoneRodizio, Clock clock) {
        this.authorize = authorize;
        this.overview = overview;
        this.sales = sales;
        this.menu = menu;
        this.kitchen = kitchen;
        this.team = team;
        this.tables = tables;
        this.phoneRodizio = phoneRodizio;
        this.clock = clock;
    }

    private static ReportQuery query(Jwt jwt, LocalDate from, LocalDate to, UUID sectorId) {
        return new ReportQuery(AuthenticatedUser.from(jwt).establishmentId(), from, to,
                sectorId == null ? null : new SectorId(sectorId));
    }

    /** Confere o plano, monta a planilha e devolve como anexo ("ohchefe-vendas-2026-11-01-a-2026-11-30.xlsx"). */
    private ResponseEntity<byte[]> export(ReportQuery query, String name, ExportFormat format, Supplier<Sheet> build) {
        authorize.authorize(query.establishmentId());
        Sheet sheet = build.get();
        byte[] body = switch (format) {
            case CSV -> CsvFormat.bytes(sheet);
            case XLSX -> XlsxFormat.bytes(sheet);
            case PDF -> PdfFormat.bytes(sheet, clock.instant());
        };
        String file = "ohchefe-%s-%s-a-%s.%s".formatted(name, query.from(), query.to(), format.extension);
        return ResponseEntity.ok()
                .contentType(format.type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file).build().toString())
                .body(body);
    }

    @GetMapping(value = "/overview/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportOverviewReport", summary = "A Visão geral em CSV, Excel ou PDF (Business)")
    @ExportResponses
    ResponseEntity<byte[]> overview(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(required = false) UUID sectorId,
                                    @RequestParam(defaultValue = "CSV") ExportFormat format) {
        ReportQuery query = query(jwt, from, to, sectorId);
        return export(query, "visao-geral", format, () -> ReportSheets.overview(overview.get(query)));
    }

    @GetMapping(value = "/sales/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportSalesReport", summary = "As Vendas em CSV, Excel ou PDF (Business)")
    @ExportResponses
    ResponseEntity<byte[]> sales(@AuthenticationPrincipal Jwt jwt,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) UUID sectorId,
                                 @RequestParam(defaultValue = "DAY") SalesReportResponse.Grouping groupBy,
                                 @RequestParam(defaultValue = "CSV") ExportFormat format) {
        ReportQuery query = query(jwt, from, to, sectorId);
        return export(query, "vendas", format,
                () -> ReportSheets.sales(sales.get(query, SeriesGrouping.valueOf(groupBy.name()))));
    }

    @GetMapping(value = "/menu/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportMenuReport",
            summary = "O Cardápio em CSV, Excel ou PDF, com todos os produtos (Business)")
    @ExportResponses
    ResponseEntity<byte[]> menu(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @RequestParam(required = false) UUID sectorId,
                                @RequestParam(required = false) UUID categoryId,
                                @RequestParam(defaultValue = "CSV") ExportFormat format) {
        MenuReportQuery query = new MenuReportQuery(query(jwt, from, to, sectorId),
                categoryId == null ? null : new MenuCategoryId(categoryId));
        return export(query.report(), "cardapio", format, () -> {
            // A tabela inteira: as páginas, uma depois da outra (a API pagina; a planilha leva tudo).
            List<MenuProduct> products = new ArrayList<>();
            ResultPage<MenuProduct> page;
            int number = 0;
            do {
                page = menu.products(query, ProductSort.REVENUE, new PageQuery(number++, PageQuery.MAX_SIZE));
                products.addAll(page.content());
            } while (number < page.totalPages());
            return ReportSheets.menu(menu.get(query), products);
        });
    }

    @GetMapping(value = "/kitchen/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportKitchenReport", summary = "A Cozinha e tempos em CSV, Excel ou PDF (Business)")
    @ExportResponses
    ResponseEntity<byte[]> kitchen(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) UUID sectorId,
                                   @RequestParam(required = false) UUID categoryId,
                                   @RequestParam(defaultValue = "CSV") ExportFormat format) {
        ReportQuery query = query(jwt, from, to, sectorId);
        return export(query, "cozinha", format, () -> ReportSheets.kitchen(kitchen.get(new KitchenReportQuery(query,
                categoryId == null ? null : new MenuCategoryId(categoryId)))));
    }

    @GetMapping(value = "/team/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportTeamReport", summary = "O Atendimento e equipe em CSV, Excel ou PDF (Business)")
    @ExportResponses
    ResponseEntity<byte[]> team(@AuthenticationPrincipal Jwt jwt,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @RequestParam(required = false) UUID sectorId,
                                @RequestParam(defaultValue = "CSV") ExportFormat format) {
        ReportQuery query = query(jwt, from, to, sectorId);
        return export(query, "equipe", format, () -> ReportSheets.team(team.get(query)));
    }

    @GetMapping(value = "/tables/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportTableReport", summary = "As Mesas e ocupação em CSV, Excel ou PDF (Business)")
    @ExportResponses
    ResponseEntity<byte[]> tables(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) UUID sectorId,
                                  @RequestParam(defaultValue = "CSV") ExportFormat format) {
        ReportQuery query = query(jwt, from, to, sectorId);
        return export(query, "mesas", format, () -> {
            var report = tables.get(query);
            return ReportSheets.tables(report, report.period().zone());
        });
    }

    @GetMapping(value = "/phone-and-rodizio/export", produces = {"text/csv", XLSX_TYPE, PDF_TYPE, "application/problem+json"})
    @Operation(operationId = "exportPhoneRodizioReport", summary = "O Celular e rodízio em CSV, Excel ou PDF (Business)")
    @ExportResponses
    ResponseEntity<byte[]> phones(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) UUID sectorId,
                                  @RequestParam(defaultValue = "CSV") ExportFormat format) {
        ReportQuery query = query(jwt, from, to, sectorId);
        return export(query, "celular-e-rodizio", format, () -> ReportSheets.phoneAndRodizio(phoneRodizio.get(query)));
    }
}
