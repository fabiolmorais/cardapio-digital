package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.report.ManageRevenueGoalsUseCase;
import br.com.cardapio.application.port.in.report.ManageRevenueGoalsUseCase.GoalChange;
import br.com.cardapio.application.port.in.report.RevenueGoals;
import br.com.cardapio.domain.report.model.RevenueGoal;
import br.com.cardapio.domain.shared.model.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** A meta de faturamento (REL-79): só o dono e os gerentes, só no Business. */
@RestController
@RequestMapping("/api/v1/reports/goals")
@Tag(name = "Relatórios", description = "Vendas, cardápio, cozinha, equipe e salão, por período")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class RevenueGoalsController {

    @Schema(name = "RevenueGoal", description = "A meta de um mês; sem valor, o mês não tem meta")
    record Goal(@Schema(requiredMode = REQUIRED, type = "string", example = "2026-11") @NotNull YearMonth month,
                @Schema(description = "Em centavos; null tira a meta")
                @Min(1) @Max(RevenueGoal.MAX_CENTS) Long amountCents) {

        static Goal of(YearMonth month, Optional<Money> amount) {
            return new Goal(month, amount.map(Money::cents).orElse(null));
        }
    }

    @Schema(name = "RevenueGoals", description = "A meta deste mês e a do próximo, no fuso do restaurante")
    record Goals(@Schema(requiredMode = REQUIRED) Goal current, @Schema(requiredMode = REQUIRED) Goal next) {

        static Goals of(RevenueGoals g) {
            return new Goals(Goal.of(g.current(), g.currentAmount()), Goal.of(g.next(), g.nextAmount()));
        }
    }

    @Schema(name = "RevenueGoalsRequest", description = "As metas a mudar (deste mês e/ou do próximo)")
    record GoalsRequest(@Schema(requiredMode = REQUIRED) @NotNull @Size(max = 2) List<@Valid @NotNull Goal> goals) {
    }

    private final ManageRevenueGoalsUseCase goals;

    RevenueGoalsController(ManageRevenueGoalsUseCase goals) {
        this.goals = goals;
    }

    @GetMapping
    @Operation(operationId = "getRevenueGoals", summary = "A meta deste mês e a do próximo (Business)")
    @ApiResponse(responseCode = "200", description = "As metas")
    @ApiResponse(responseCode = "403", description = "O plano não tem a meta (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    Goals get(@AuthenticationPrincipal Jwt jwt) {
        return Goals.of(goals.get(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PutMapping
    @Operation(operationId = "updateRevenueGoals", summary = "Muda a meta deste mês e/ou a do próximo (Business)")
    @ApiResponse(responseCode = "200", description = "Salva")
    @ApiResponse(responseCode = "400", description = "Valor fora da faixa ou mês que não é este nem o próximo",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O plano não tem a meta (é do Business)", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    Goals update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GoalsRequest body) {
        return Goals.of(goals.update(AuthenticatedUser.from(jwt).establishmentId(), body.goals().stream()
                .map(g -> new GoalChange(g.month(), Optional.ofNullable(g.amountCents()).map(Money::ofCents)))
                .toList()));
    }
}
