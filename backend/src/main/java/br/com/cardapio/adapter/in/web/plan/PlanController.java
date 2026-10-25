package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/plans")
@Tag(name = "Planos", description = "Planos de assinatura oferecidos aos restaurantes")
class PlanController {

    private final ListPlansUseCase listPlans;

    PlanController(ListPlansUseCase listPlans) {
        this.listPlans = listPlans;
    }

    @GetMapping
    @Operation(
            operationId = "listPlans",
            summary = "Lista os planos de assinatura",
            description = "Público, sem autenticação. Retorna os planos ativos na ordem de exibição. "
                    + "Preços em centavos; campos nulos significam \"sob consulta\" (preço) ou \"ilimitado\" (limites).")
    @ApiResponse(responseCode = "200", description = "Página de planos")
    @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<PlanResponse> list(
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(
                listPlans.listActivePlans(new PageQuery(page, size)),
                PlanWebMapper::toResponse);
    }
}
