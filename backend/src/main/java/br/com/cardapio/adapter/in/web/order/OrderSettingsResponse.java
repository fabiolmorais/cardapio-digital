package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.domain.order.model.OrderSettings;
import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "OrderSettings")
record OrderSettingsResponse(
        @Schema(requiredMode = REQUIRED) boolean orderingEnabled,
        @Schema(requiredMode = REQUIRED) boolean approvalRequired,
        @Schema(requiredMode = REQUIRED) boolean staffOrderingEnabled,
        Long maxAmountCents,
        Integer maxItems,
        @Schema(requiredMode = REQUIRED, description = "A taxa de serviço opcional")
        OrderSettingsRequest.ServiceChargeRequest serviceCharge
) {
    static OrderSettingsResponse from(OrderSettings s) {
        return new OrderSettingsResponse(s.orderingEnabled(), s.approvalRequired(), s.staffOrderingEnabled(),
                s.maxAmount() == null ? null : s.maxAmount().cents(), s.maxItems(),
                new OrderSettingsRequest.ServiceChargeRequest(s.serviceCharge().enabled(), s.serviceCharge().percent(),
                        s.serviceCharge().destination(), s.serviceCharge().customAllowed()));
    }
}
