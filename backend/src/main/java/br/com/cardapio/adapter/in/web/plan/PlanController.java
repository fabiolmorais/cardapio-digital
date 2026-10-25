package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.domain.shared.model.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/plans")
@Tag(name = "Planos")
class PlanController {

    private final ListPlansUseCase listPlans;

    PlanController(ListPlansUseCase listPlans) {
        this.listPlans = listPlans;
    }

    @GetMapping
    @Operation(operationId = "listPlans", summary = "Lista os planos de assinatura (público)")
    PageResponse<PlanResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(
                listPlans.listActivePlans(new PageQuery(page, size)),
                PlanWebMapper::toResponse);
    }
}
