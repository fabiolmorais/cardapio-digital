package br.com.cardapio.adapter.in.web.rating;

import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.rating.RateServiceUseCase;
import br.com.cardapio.application.port.in.rating.RateServiceUseCase.RateServiceCommand;
import br.com.cardapio.application.port.in.rating.RateServiceUseCase.RatingOffer;
import br.com.cardapio.domain.rating.model.RatingTag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * "Como foi o atendimento?" (RF40), no celular da mesa: depois de pedir a conta, ou logo depois de a
 * comanda fechar. Sem login: o cookie table_session diz qual celular é; uma avaliação por celular.
 */
@RestController
@RequestMapping("/api/v1/public/tables/{token}/rating")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicRatingController {

    private final RateServiceUseCase rate;

    PublicRatingController(RateServiceUseCase rate) {
        this.rate = rate;
    }

    @Schema(name = "TableRatingOffer", description = "Se este celular pode avaliar o atendimento agora")
    record OfferResponse(
            @Schema(requiredMode = REQUIRED, description = "Pode avaliar agora (e ainda não avaliou)") boolean available,
            @Schema(description = "A nota que este celular deu; ausente se ainda não avaliou") Integer stars,
            @Schema(requiredMode = REQUIRED, description = "Avaliou com 4 ou 5: mostra o convite ao Google")
            boolean googleInvite,
            @Schema(description = "O primeiro nome de quem mais atendeu a mesa") String waiterName) {
        static OfferResponse from(RatingOffer o) {
            return new OfferResponse(o.available(), o.stars(), o.googleInvite(), o.waiterName());
        }
    }

    @Schema(name = "RateServiceRequest")
    record RateRequest(
            @Schema(requiredMode = REQUIRED, description = "De 1 a 5 estrelas", example = "5")
            @NotNull @Min(1) @Max(5) Integer stars,
            @Schema(description = "Os atalhos marcados") @Size(max = 4) Set<RatingTag> tags,
            @Schema(description = "Comentário curto, opcional") @Size(max = 280) String comment) {
    }

    @GetMapping
    @Operation(operationId = "getTableRating", summary = "Se este celular pode avaliar o atendimento")
    @ApiResponse(responseCode = "200", description = "Disponível, já avaliado, ou ainda não")
    @ApiResponse(responseCode = "401", description = "Sem sessão desta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    OfferResponse offer(@PathVariable String token,
                        @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        return OfferResponse.from(rate.offer(token, session));
    }

    @PostMapping
    @Operation(operationId = "rateService", summary = "Avalia o atendimento: de 1 a 5, os atalhos e um comentário")
    @ApiResponse(responseCode = "201", description = "Avaliado; com 4 ou 5, a resposta traz o convite ao Google")
    @ApiResponse(responseCode = "400", description = "Nota fora de 1 a 5 ou comentário longo demais",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Sem sessão desta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Ainda não pode avaliar (sem pedir a conta), ou já avaliou",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<OfferResponse> rate(@PathVariable String token,
                                       @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
                                       @Valid @RequestBody RateRequest request) {
        RatingOffer result = rate.rate(new RateServiceCommand(token, session, request.stars(), request.tags(),
                request.comment()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferResponse.from(result));
    }

    @PostMapping("/google")
    @Operation(operationId = "clickRatingGoogle", summary = "O cliente tocou em \"Avaliar no Google\"")
    @ApiResponse(responseCode = "204", description = "Contado (uma vez por celular)")
    @ApiResponse(responseCode = "401", description = "Sem sessão desta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Não avaliou, ou a nota não teve o convite",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> clickGoogle(@PathVariable String token,
                                     @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        rate.clickGoogle(token, session);
        return ResponseEntity.noContent().build();
    }
}
