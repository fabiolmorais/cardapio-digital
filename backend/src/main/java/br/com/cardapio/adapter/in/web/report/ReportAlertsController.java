package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.report.ManageReportAlertsUseCase;
import br.com.cardapio.domain.report.model.AlertSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Os alertas em tempo real (REL-78): só o dono e os gerentes, só no Business. */
@RestController
@RequestMapping("/api/v1/reports/alerts")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class ReportAlertsController {

    @Schema(name = "ReportAlerts", description = "Os alertas em tempo real: cada um liga e desliga e tem o limite")
    record Alerts(@Schema(requiredMode = REQUIRED) boolean kitchenEnabled,
                  @Schema(requiredMode = REQUIRED, description = "Itens em preparo há mais de (min)")
                  @Min(AlertSettings.MIN_KITCHEN) @Max(AlertSettings.MAX_KITCHEN) int kitchenMinutes,
                  @Schema(requiredMode = REQUIRED) boolean callEnabled,
                  @Schema(requiredMode = REQUIRED, description = "Mesa chamando há mais de (min)")
                  @Min(AlertSettings.MIN_CALL) @Max(AlertSettings.MAX_CALL) int callMinutes,
                  @Schema(requiredMode = REQUIRED) boolean tabEnabled,
                  @Schema(requiredMode = REQUIRED, description = "Comanda aberta há mais de (h)")
                  @Min(AlertSettings.MIN_TAB) @Max(AlertSettings.MAX_TAB) int tabHours) {

        static Alerts of(AlertSettings s) {
            return new Alerts(s.kitchenEnabled(), s.kitchenMinutes(), s.callEnabled(), s.callMinutes(),
                    s.tabEnabled(), s.tabHours());
        }

        AlertSettings toSettings() {
            return new AlertSettings(kitchenEnabled, kitchenMinutes, callEnabled, callMinutes, tabEnabled, tabHours);
        }
    }

    private final ManageReportAlertsUseCase alerts;

    ReportAlertsController(ManageReportAlertsUseCase alerts) {
        this.alerts = alerts;
    }

    @GetMapping
    @Operation(operationId = "getReportAlerts", summary = "Os alertas em tempo real do restaurante (Business)")
    @ApiResponse(responseCode = "200", description = "A configuração (sem configuração, o padrão)")
    @ApiResponse(responseCode = "403", description = "O plano não tem os alertas (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    Alerts get(@AuthenticationPrincipal Jwt jwt) {
        return Alerts.of(alerts.get(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PutMapping
    @Operation(operationId = "updateReportAlerts", summary = "Liga, desliga e muda os limites dos alertas (Business)")
    @ApiResponse(responseCode = "200", description = "Salva")
    @ApiResponse(responseCode = "400", description = "Limite fora da faixa", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem os alertas (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    Alerts update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Alerts body) {
        return Alerts.of(alerts.update(AuthenticatedUser.from(jwt).establishmentId(), body.toSettings()));
    }
}
