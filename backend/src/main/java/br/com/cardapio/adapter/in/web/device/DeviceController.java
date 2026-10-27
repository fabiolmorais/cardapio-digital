package br.com.cardapio.adapter.in.web.device;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.adapter.in.web.shared.PageResponse;
import br.com.cardapio.application.port.in.device.DeviceSummary;
import br.com.cardapio.application.port.in.device.LinkDeviceUseCase;
import br.com.cardapio.application.port.in.device.ListDevicesUseCase;
import br.com.cardapio.application.port.in.device.UnlinkDeviceUseCase;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.domain.device.model.DeviceId;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/** O lado do painel: dono e gerente vinculam, listam e desvinculam os aparelhos do restaurante. */
@RestController
@Validated
@RequestMapping("/api/v1/devices")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
@Tag(name = "Aparelhos", description = "Vínculo de celulares, tablets e TVs ao restaurante")
@SecurityRequirement(name = "bearer")
class DeviceController {

    private final LinkDeviceUseCase linkDevice;
    private final ListDevicesUseCase listDevices;
    private final UnlinkDeviceUseCase unlinkDevice;

    DeviceController(LinkDeviceUseCase linkDevice, ListDevicesUseCase listDevices, UnlinkDeviceUseCase unlinkDevice) {
        this.linkDevice = linkDevice;
        this.listDevices = listDevices;
        this.unlinkDevice = unlinkDevice;
    }

    @GetMapping
    @Operation(operationId = "listDevices", summary = "Aparelhos vinculados ao restaurante, por nome")
    @ApiResponse(responseCode = "200", description = "Página de aparelhos")
    PageResponse<DeviceResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Itens por página (1 a 100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        AuthenticatedUser user = AuthenticatedUser.from(jwt);
        return PageResponse.from(listDevices.list(user.establishmentId(), new PageQuery(page, size)),
                DeviceResponse::from);
    }

    @PostMapping
    @Operation(operationId = "linkDevice", summary = "Vincula o aparelho cujo código aparece na tela dele")
    @ApiResponse(responseCode = "201", description = "Vinculado; o aparelho busca o token sozinho")
    @ApiResponse(responseCode = "400", description = "Código fora do formato ou nome vazio",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Código não encontrado ou vencido",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<DeviceResponse> link(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LinkDeviceRequest request) {
        DeviceSummary device = linkDevice.link(request.toCommand(AuthenticatedUser.from(jwt)));
        URI location = URI.create("/api/v1/devices/" + device.id().value());
        return ResponseEntity.created(location).body(DeviceResponse.from(device));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "unlinkDevice", summary = "Desvincula o aparelho; o token dele deixa de valer")
    @ApiResponse(responseCode = "204", description = "Desvinculado")
    @ApiResponse(responseCode = "404", description = "Não é um aparelho deste restaurante",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> unlink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        unlinkDevice.unlink(AuthenticatedUser.from(jwt).establishmentId(), new DeviceId(id));
        return ResponseEntity.noContent().build();
    }
}
