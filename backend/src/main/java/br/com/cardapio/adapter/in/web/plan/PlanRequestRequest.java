package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.domain.plan.model.BillingCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "PlanRequestRequest", description = "O plano e o ciclo que o dono escolheu")
record PlanRequestRequest(
        @Schema(requiredMode = REQUIRED, example = "pro") @NotBlank @Size(max = 40) String planCode,
        @Schema(requiredMode = REQUIRED) @NotNull BillingCycle billingCycle) {
}
