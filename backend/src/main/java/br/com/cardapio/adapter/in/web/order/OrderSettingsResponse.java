package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.order.model.OrderSettings;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "OrderSettings")
record OrderSettingsResponse(
        @Schema(requiredMode = REQUIRED) boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED) boolean approvalRequired,
        Long maxAmountCents,
        Integer maxItems
) {
    static OrderSettingsResponse from(OrderSettings s) {
        return new OrderSettingsResponse(s.orderingEnabled(), s.approvalRequired(),
                s.maxAmount() == null ? null : s.maxAmount().cents(), s.maxItems());
    }
}
