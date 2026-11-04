package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.rodizio.QuoteRodiziosUseCase;
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
import org.springframework.web.bind.annotation.*;

/** "Escolha o rodízio": os rodízios do restaurante, com o preço por pessoa de agora. */
@RestController
@Validated
@RequestMapping("/api/v1/public/tables/{token}")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicRodizioController {

    private final QuoteRodiziosUseCase quotes;

    PublicRodizioController(QuoteRodiziosUseCase quotes) {
        this.quotes = quotes;
    }

    @GetMapping("/rodizios")
    @Operation(operationId = "listTableRodizios", summary = "Os rodízios do restaurante, com o preço de agora")
    @ApiResponse(responseCode = "200", description = "Página dos rodízios")
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<RodizioQuoteResponse> list(
            @PathVariable String token,
            @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Rodízios por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(quotes.forTable(token, session, new PageQuery(page, size)),
                RodizioQuoteResponse::from);
    }
}
