package br.com.cardapio.adapter.in.web.rating;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.rating.ManageRatingsUseCase;
import br.com.cardapio.domain.rating.model.RatingSummary;
import br.com.cardapio.domain.rating.model.RatingView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.rating.model.RatingId;
import br.com.cardapio.domain.rating.model.RatingTag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * As avaliações no painel (RF40): as de hoje e os alertas de nota baixa (dono e gerente), a média do
 * garçom (quem está logado, se o restaurante mostra) e a opção que liga essa média.
 */
@RestController
@Validated
@RequestMapping("/api/v1/ratings")
@Tag(name = "Avaliações", description = "A avaliação do atendimento pelas mesas")
@SecurityRequirement(name = "bearer")
class StaffRatingController {

    private final ManageRatingsUseCase ratings;

    StaffRatingController(ManageRatingsUseCase ratings) {
        this.ratings = ratings;
    }

    @Schema(name = "RatingSummary", description = "O resumo das avaliações")
    record SummaryResponse(
            @Schema(requiredMode = REQUIRED) int count,
            @Schema(description = "A nota média, com uma casa; ausente sem avaliações") Double average,
            @Schema(requiredMode = REQUIRED, description = "Notas 1 e 2") int low,
            @Schema(requiredMode = REQUIRED, description = "Notas 4 e 5 (as que viram o convite ao Google)") int high,
            @Schema(requiredMode = REQUIRED, description = "Quantos tocaram em \"Avaliar no Google\"")
            int googleClicks) {
        static SummaryResponse from(RatingSummary s) {
            return new SummaryResponse(s.count(),
                    s.average() == null ? null : Math.round(s.average() * 10) / 10.0, s.low(), s.high(),
                    s.googleClicks());
        }
    }

    @Schema(name = "Rating", description = "Uma avaliação, com a mesa e quem atendeu")
    record RatingResponse(
            @Schema(requiredMode = REQUIRED) UUID id,
            @Schema(requiredMode = REQUIRED) String tableLabel,
            @Schema(description = "Quem mais atendeu a mesa; ausente se ninguém da equipe atendeu") String waiterName,
            @Schema(requiredMode = REQUIRED) int stars,
            @Schema(requiredMode = REQUIRED) List<RatingTag> tags,
            String comment,
            @Schema(requiredMode = REQUIRED) Instant createdAt,
            @Schema(requiredMode = REQUIRED, description = "Tocou em \"Avaliar no Google\"") boolean google,
            @Schema(description = "Nota baixa: quando o alerta foi resolvido") Instant resolvedAt) {
        static RatingResponse from(RatingView v) {
            var r = v.rating();
            return new RatingResponse(r.id().value(), v.tableLabel(), v.waiterName(), r.stars(),
                    r.tags().stream().sorted().toList(), r.comment(), r.createdAt(), r.googleClickedAt() != null,
                    r.resolvedAt());
        }
    }

    @Schema(name = "RatingSettings")
    record SettingsBody(@Schema(requiredMode = REQUIRED, description = "O garçom vê a própria média no app")
                        boolean waiterSeesAverage) {
    }

    @Schema(name = "MyRating", description = "A média de quem está logado nos últimos 30 dias")
    record MineResponse(
            @Schema(requiredMode = REQUIRED, description = "O restaurante mostra a média ao garçom") boolean visible,
            @Schema(description = "Ausente se o restaurante não mostra") SummaryResponse summary) {
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "getTodayRatings", summary = "O resumo das avaliações de hoje")
    SummaryResponse today(@AuthenticationPrincipal Jwt jwt) {
        return SummaryResponse.from(ratings.today(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "listTodayRatings", summary = "As avaliações de hoje, da mais nova para a mais velha")
    PageResponse<RatingResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Só as notas 1 e 2") @RequestParam(defaultValue = "false") boolean onlyLow,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(ratings.todayList(AuthenticatedUser.from(jwt).establishmentId(), onlyLow,
                new PageQuery(page, size)), RatingResponse::from);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "listRatingAlerts",
            summary = "As notas baixas de hoje que ninguém resolveu (no máximo 20, lista curta)")
    List<RatingResponse> alerts(@AuthenticationPrincipal Jwt jwt) {
        return ratings.openAlerts(AuthenticatedUser.from(jwt).establishmentId()).stream()
                .map(RatingResponse::from).toList();
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "resolveRating", summary = "\"Já resolvi\": o alerta de nota baixa sai do painel")
    @ApiResponse(responseCode = "204", description = "Resolvido")
    @ApiResponse(responseCode = "404", description = "Não é uma avaliação deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Não é nota baixa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> resolve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var user = AuthenticatedUser.from(jwt);
        ratings.resolve(user.establishmentId(), new RatingId(id), user.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
    @Operation(operationId = "getMyRating", summary = "A própria média, se o restaurante mostra ao garçom")
    MineResponse mine(@AuthenticationPrincipal Jwt jwt) {
        var user = AuthenticatedUser.from(jwt);
        RatingSummary summary = ratings.mine(user.establishmentId(), user.userId());
        return new MineResponse(summary != null, summary == null ? null : SummaryResponse.from(summary));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "getRatingSettings", summary = "O que o garçom vê das avaliações")
    SettingsBody settings(@AuthenticationPrincipal Jwt jwt) {
        return new SettingsBody(ratings.waiterSeesAverage(AuthenticatedUser.from(jwt).establishmentId()));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(operationId = "updateRatingSettings", summary = "Liga ou desliga a média do garçom no app")
    SettingsBody updateSettings(@AuthenticationPrincipal Jwt jwt, @RequestBody SettingsBody body) {
        var id = AuthenticatedUser.from(jwt).establishmentId();
        ratings.setWaiterSeesAverage(id, body.waiterSeesAverage());
        return new SettingsBody(ratings.waiterSeesAverage(id));
    }
}
