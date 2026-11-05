package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.plan.GetSubscriptionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscription")
@Tag(name = "Assinatura", description = "O plano do restaurante, o teste grátis e os limites")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
class SubscriptionController {

    private final GetSubscriptionUseCase subscription;

    SubscriptionController(GetSubscriptionUseCase subscription) {
        this.subscription = subscription;
    }

    @GetMapping
    @Operation(operationId = "getSubscription", summary = "A assinatura deste restaurante, com o uso dos limites")
    @ApiResponse(responseCode = "200", description = "A assinatura")
    SubscriptionResponse get(@AuthenticationPrincipal Jwt jwt) {
        return SubscriptionResponse.from(subscription.get(AuthenticatedUser.from(jwt).establishmentId()));
    }
}
