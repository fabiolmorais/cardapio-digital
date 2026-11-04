package br.com.cardapio.adapter.in.web.rodizio;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.rodizio.ActivateRodizioCommand;
import br.com.cardapio.application.port.in.rodizio.ActivateRodizioUseCase;
import br.com.cardapio.application.port.in.rodizio.ChangeRodizioGuestsUseCase;
import br.com.cardapio.application.port.in.rodizio.QuoteRodiziosUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.rodizio.model.RodizioId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** O rodízio na mesa, pelo lado da equipe: os preços de agora, ativar e ajustar as pessoas. */
@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Rodízio", description = "O cadastro dos rodízios: faixas, preços, produtos e regras")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF')")
class TableRodizioController {

    private final QuoteRodiziosUseCase quotes;
    private final ActivateRodizioUseCase activate;
    private final ChangeRodizioGuestsUseCase changeGuests;

    TableRodizioController(QuoteRodiziosUseCase quotes, ActivateRodizioUseCase activate,
                           ChangeRodizioGuestsUseCase changeGuests) {
        this.quotes = quotes;
        this.activate = activate;
        this.changeGuests = changeGuests;
    }

    @GetMapping("/rodizio-quotes")
    @Operation(operationId = "quoteRodizios", summary = "Os rodízios ativos com o preço de cada faixa agora")
    @ApiResponse(responseCode = "200", description = "Página dos rodízios")
    PageResponse<RodizioQuoteResponse> quote(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Rodízios por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(quotes.forStaff(AuthenticatedUser.from(jwt).establishmentId(),
                new PageQuery(page, size)), RodizioQuoteResponse::from);
    }

    @PostMapping("/tables/{tableId}/rodizio")
    @Operation(operationId = "activateRodizio",
            summary = "Ativa o rodízio na mesa com as pessoas por faixa (abre a comanda, se preciso)")
    @ApiResponse(responseCode = "201", description = "Ativado: o preço de agora foi copiado")
    @ApiResponse(responseCode = "400", description = "Campo inválido, nenhuma pessoa, ou faixa que não existe",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Mesa ou rodízio que não é deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A mesa já está num rodízio",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Rodízio de passagem (ainda não funciona na mesa)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TableRodizioResponse> activate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tableId,
                                                  @Valid @RequestBody ActivateRodizioRequest request) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        var activated = activate.activate(new ActivateRodizioCommand(user.establishmentId(),
                new DiningTableId(tableId), new RodizioId(request.rodizioId()), request.guestMap(), user.userId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TableRodizioResponse.from(activated));
    }

    @PutMapping("/tables/{tableId}/rodizio/guests")
    @Operation(operationId = "changeRodizioGuests", summary = "Ajusta as pessoas do rodízio da mesa (até a conta)")
    @ApiResponse(responseCode = "200", description = "Ajustado: os preços continuam os da ativação")
    @ApiResponse(responseCode = "400", description = "Campo inválido, nenhuma pessoa, ou faixa que não existe",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Mesa que não é deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A mesa não está no rodízio",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TableRodizioResponse changeGuests(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tableId,
                                      @Valid @RequestBody GuestsRequest request) {
        return TableRodizioResponse.from(changeGuests.change(AuthenticatedUser.from(jwt).establishmentId(),
                new DiningTableId(tableId), request.toMap()));
    }
}
