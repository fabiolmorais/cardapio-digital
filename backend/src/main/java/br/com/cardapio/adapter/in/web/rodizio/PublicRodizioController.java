package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.rodizio.QuoteRodiziosUseCase;
import br.com.cardapio.application.port.in.rodizio.TurnRodizioCardUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final TurnRodizioCardUseCase turnCard;

    PublicRodizioController(QuoteRodiziosUseCase quotes, TurnRodizioCardUseCase turnCard) {
        this.quotes = quotes;
        this.turnCard = turnCard;
    }

    @PutMapping("/rodizio/card")
    @Operation(operationId = "turnRodizioCard",
            summary = "Vira o cartão do rodízio de passagem: verde (pode servir) ou vermelho (pausa)")
    @ApiResponse(responseCode = "200", description = "O rodízio da mesa, com a cor nova")
    @ApiResponse(responseCode = "403", description = "Sem presença confirmada, ou celular bloqueado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A mesa não está num rodízio de passagem",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TableRodizioResponse turnCard(@PathVariable String token,
                                  @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
                                  @Valid @RequestBody CardRequest request) {
        return TableRodizioResponse.from(turnCard.turn(token, session, request.status()));
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
