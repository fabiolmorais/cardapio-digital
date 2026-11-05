package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.application.port.in.plan.SubscriptionOverview;
import br.com.cardapio.domain.plan.model.BillingCycle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Subscription", description = "A assinatura do restaurante: plano, situação, datas e uso dos limites")
public record SubscriptionResponse(
        @Schema(requiredMode = REQUIRED) PlanResponse plan,
        @Schema(requiredMode = REQUIRED, description = """
                TRIAL: teste grátis · ACTIVE: paga · PENDING: venceu, na tolerância (tudo funciona) ·
                SUSPENDED: passou da tolerância (a mesa para de chamar e pedir)""")
        StandingDto standing,
        @Schema(requiredMode = REQUIRED, description = "O fim do teste grátis ou do período pago")
        Instant coveredUntil,
        @Schema(requiredMode = REQUIRED, description = "O fim da tolerância, depois de vencer")
        Instant graceEndsAt,
        @Schema(requiredMode = REQUIRED) Usage usage,
        @Schema(requiredMode = REQUIRED, nullable = true,
                description = "O plano escolhido pelo dono, à espera do pagamento; null se não escolheu")
        Request request) {

    public enum StandingDto {
        TRIAL, ACTIVE, PENDING, SUSPENDED
    }

    @Schema(name = "PlanUsage", description = "Quanto o restaurante usa agora (só os ativos); os limites estão no plano")
    public record Usage(
            @Schema(requiredMode = REQUIRED) long tables,
            @Schema(requiredMode = REQUIRED) long users,
            @Schema(requiredMode = REQUIRED) long sectors) {
    }

    @Schema(name = "PlanRequest", description = "O plano escolhido, à espera do pagamento")
    public record Request(
            @Schema(requiredMode = REQUIRED, example = "pro") String planCode,
            @Schema(requiredMode = REQUIRED, example = "Pro") String planName,
            @Schema(requiredMode = REQUIRED) BillingCycle billingCycle,
            @Schema(requiredMode = REQUIRED) Instant requestedAt) {
    }

    static SubscriptionResponse from(SubscriptionOverview o) {
        var r = o.request();
        return new SubscriptionResponse(PlanWebMapper.toResponse(o.plan()), StandingDto.valueOf(o.standing().name()),
                o.coveredUntil(), o.graceEndsAt(),
                new Usage(o.usage().tables(), o.usage().users(), o.usage().sectors()),
                r == null ? null : new Request(r.plan().code(), r.plan().name(), r.cycle(), r.requestedAt()));
    }
}
