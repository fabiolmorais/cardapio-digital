package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.application.port.in.device.ClaimPairingUseCase;
import br.com.cardapio.application.port.in.device.ClaimResult;
import br.com.cardapio.application.port.in.device.PairingStarted;
import br.com.cardapio.application.port.in.device.StartPairingUseCase;
import br.com.cardapio.domain.device.model.PairingId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * O lado do aparelho novo. Público: o aparelho ainda não é de ninguém. Quem prova a identidade
 * dele é o segredo recebido ao abrir o pedido.
 */
@RestController
@RequestMapping("/api/v1/device-pairings")
@Tag(name = "Aparelhos", description = "Vínculo de celulares, tablets e TVs ao restaurante")
class DevicePairingController {

    private final StartPairingUseCase startPairing;
    private final ClaimPairingUseCase claimPairing;
    private final DeviceTokenCookie cookie;

    DevicePairingController(StartPairingUseCase startPairing, ClaimPairingUseCase claimPairing,
                            DeviceTokenCookie cookie) {
        this.startPairing = startPairing;
        this.claimPairing = claimPairing;
        this.cookie = cookie;
    }

    @PostMapping
    @Operation(operationId = "startDevicePairing", summary = "Abre um pedido de vínculo e devolve o código para a tela")
    @ApiResponse(responseCode = "201", description = "Pedido aberto")
    ResponseEntity<PairingStartedResponse> start() {
        PairingStarted started = startPairing.start();
        URI location = URI.create("/api/v1/device-pairings/" + started.id().value());
        return ResponseEntity.created(location).body(PairingStartedResponse.from(started));
    }

    @PostMapping("/{id}/claim")
    @Operation(operationId = "claimDevicePairing",
            summary = "O aparelho pergunta se já foi vinculado; se sim, recebe o token no cookie device_token")
    @ApiResponse(responseCode = "200", description = "PENDING ou LINKED")
    @ApiResponse(responseCode = "404", description = "Pedido desconhecido, vencido, segredo errado ou token já entregue",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<ClaimPairingResponse> claim(@PathVariable UUID id, @Valid @RequestBody ClaimPairingRequest request) {
        ClaimResult result = claimPairing.claim(new PairingId(id), request.secret());
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result instanceof ClaimResult.Linked linked) {
            response.header(HttpHeaders.SET_COOKIE, cookie.create(linked.deviceToken()).toString());
        }
        return response.body(ClaimPairingResponse.from(result));
    }
}
