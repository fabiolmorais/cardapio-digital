package br.com.cardapio.adapter.in.web.plan;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "Plan", description = "Plano de assinatura")
public record PlanResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED, example = "pro") String code,
        @Schema(requiredMode = REQUIRED, example = "Pro") String name,
        @Schema(requiredMode = REQUIRED, nullable = true, description = "Preço mensal em centavos; nulo = sob consulta")
        Long monthlyPriceCents,
        @Schema(requiredMode = REQUIRED, nullable = true, description = "Preço anual em centavos; nulo = sob consulta")
        Long annualPriceCents,
        @Schema(requiredMode = REQUIRED, nullable = true, description = "Nulo = ilimitado") Integer maxTables,
        @Schema(requiredMode = REQUIRED, nullable = true, description = "Nulo = ilimitado") Integer maxUsers,
        @Schema(requiredMode = REQUIRED, nullable = true, description = "Nulo = ilimitado") Integer maxSectors,
        @Schema(requiredMode = REQUIRED, nullable = true, description = "Dias de histórico; nulo = ilimitado")
        Integer historyDays,
        @Schema(requiredMode = REQUIRED) List<PlanFeatureDto> features,
        @Schema(requiredMode = REQUIRED) SupportLevelDto support) {

    public enum PlanFeatureDto {
        PRODUCT_VIDEOS, PREP_STATIONS, ROLES_AND_PERMISSIONS, ADVANCED_REPORTS, REPORT_EXPORT, MULTI_UNIT
    }

    public enum SupportLevelDto {
        EMAIL, WHATSAPP, PRIORITY_WHATSAPP, DEDICATED_MANAGER
    }
}
