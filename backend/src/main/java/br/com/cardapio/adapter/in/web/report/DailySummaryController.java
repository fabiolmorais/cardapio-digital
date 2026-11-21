package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.DailySummaryDtos.Settings;
import br.com.cardapio.adapter.in.web.report.DailySummaryDtos.SettingsRequest;
import br.com.cardapio.adapter.in.web.report.DailySummaryDtos.Summary;
import br.com.cardapio.adapter.in.web.report.DailySummaryDtos.TestResult;
import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.report.ManageDailySummaryUseCase;
import br.com.cardapio.application.port.in.report.UpdateDailySummaryCommand;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.report.model.DailySummarySettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/** O resumo diário dos relatórios (REL-76): só o dono e os gerentes, Pro em diante. */
@RestController
@RequestMapping("/api/v1/reports/daily-summary")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class DailySummaryController {

    private final ManageDailySummaryUseCase summaries;

    DailySummaryController(ManageDailySummaryUseCase summaries) {
        this.summaries = summaries;
    }

    @GetMapping("/settings")
    @Operation(operationId = "getDailySummarySettings", summary = "Como o resumo diário está e quem pode receber")
    @ApiResponse(responseCode = "200", description = "A configuração")
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    Settings settings(@AuthenticationPrincipal Jwt jwt) {
        return Settings.of(summaries.get(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PutMapping("/settings")
    @Operation(operationId = "updateDailySummarySettings", summary = "Troca o horário, os canais e quem recebe")
    @ApiResponse(responseCode = "200", description = "Salva")
    @ApiResponse(responseCode = "400", description = "Hora fora de 0 a 23", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Quem recebe não é dono nem gerente do restaurante",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    Settings update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SettingsRequest body) {
        return Settings.of(summaries.update(new UpdateDailySummaryCommand(AuthenticatedUser.from(jwt).establishmentId(),
                new DailySummarySettings(body.emailEnabled(), body.pushEnabled(), body.sendHour()),
                body.receiverIds().stream().map(UserId::new).collect(Collectors.toSet()))));
    }

    @GetMapping("/preview")
    @Operation(operationId = "previewDailySummary", summary = "O resumo do último dia que já terminou")
    @ApiResponse(responseCode = "200", description = "O resumo, como sairia agora")
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    Summary preview(@AuthenticationPrincipal Jwt jwt) {
        return Summary.of(summaries.preview(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PostMapping("/test")
    @Operation(operationId = "sendDailySummaryTest", summary = "Manda o resumo do último dia só para mim")
    @ApiResponse(responseCode = "200", description = "Para onde o teste saiu")
    @ApiResponse(responseCode = "403", description = "O plano não tem relatórios", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TestResult test(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return TestResult.of(summaries.sendTest(user.establishmentId(), user.userId()));
    }
}
