package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.plan.GetSubscriptionUseCase;
import br.com.cardapio.application.port.in.plan.RequestPlanCommand;
import br.com.cardapio.application.port.in.plan.RequestPlanUseCase;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscription")
@Tag(name = "Assinatura", description = "O plano do restaurante, o teste grátis e os limites")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class SubscriptionController {

    private final GetSubscriptionUseCase subscription;
    private final RequestPlanUseCase requestPlan;

    SubscriptionController(GetSubscriptionUseCase subscription, RequestPlanUseCase requestPlan) {
        this.subscription = subscription;
        this.requestPlan = requestPlan;
    }

    @GetMapping
    @Operation(operationId = "getSubscription", summary = "A assinatura deste restaurante, com o uso dos limites")
    @ApiResponse(responseCode = "200", description = "A assinatura")
    SubscriptionResponse get(@AuthenticationPrincipal Jwt jwt) {
        return SubscriptionResponse.from(subscription.get(AuthenticatedUser.from(jwt).establishmentId()));
    }

    /** Escolher o plano é decisão de quem paga: só o dono. */
    @PutMapping("/plan-request")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(operationId = "requestPlan", summary = "Escolhe o plano e o ciclo (vale depois do pagamento)")
    @ApiResponse(responseCode = "200", description = "A assinatura, com o plano escolhido")
    @ApiResponse(responseCode = "404", description = "Plano não encontrado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Plano sob consulta (Enterprise)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    SubscriptionResponse requestPlan(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanRequestRequest request) {
        return SubscriptionResponse.from(requestPlan.request(new RequestPlanCommand(
                AuthenticatedUser.from(jwt).establishmentId(), request.planCode(), request.billingCycle())));
    }
}
