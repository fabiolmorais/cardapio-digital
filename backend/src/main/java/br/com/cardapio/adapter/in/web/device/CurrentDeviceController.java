package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.device.CurrentDeviceUseCase;
import br.com.cardapio.application.port.in.team.ListPinMembersUseCase;
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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tela do PIN, no aparelho vinculado. Fica em /api/v1/auth porque o cookie device_token só
 * vai para esse caminho; quem identifica o aparelho é o cookie, não um login.
 */
@RestController
@Validated
@RequestMapping("/api/v1/auth/device")
@Tag(name = "Aparelhos", description = "Vínculo de celulares, tablets e TVs ao restaurante")
@ApiResponse(responseCode = "401", description = "Aparelho não vinculado (sem cookie, cookie desconhecido ou desvinculado)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
class CurrentDeviceController {

    private final CurrentDeviceUseCase currentDevice;
    private final ListPinMembersUseCase pinMembers;

    CurrentDeviceController(CurrentDeviceUseCase currentDevice, ListPinMembersUseCase pinMembers) {
        this.currentDevice = currentDevice;
        this.pinMembers = pinMembers;
    }

    @GetMapping
    @Operation(operationId = "getCurrentDevice", summary = "Este aparelho e o restaurante dele")
    @ApiResponse(responseCode = "200", description = "Aparelho vinculado")
    CurrentDeviceResponse current(@CookieValue(name = DeviceTokenCookie.NAME, required = false) String deviceToken) {
        return CurrentDeviceResponse.from(currentDevice.current(deviceToken));
    }

    @GetMapping("/team-members")
    @Operation(operationId = "listPinMembers", summary = "Quem pode entrar por PIN neste aparelho, por nome")
    @ApiResponse(responseCode = "200", description = "Página de pessoas")
    @ApiResponse(responseCode = "403", description = "O aparelho é uma TV",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<PinMemberResponse> teamMembers(
            @CookieValue(name = DeviceTokenCookie.NAME, required = false) String deviceToken,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "50") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(pinMembers.list(deviceToken, new PageQuery(page, size)), PinMemberResponse::from);
    }
}
