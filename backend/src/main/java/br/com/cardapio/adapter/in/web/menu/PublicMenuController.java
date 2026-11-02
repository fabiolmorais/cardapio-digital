package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.application.port.in.menu.GetPublicMenuUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O cardápio da página da mesa. Público, como a página: quem diz o restaurante é o token da mesa. */
@RestController
@RequestMapping("/api/v1/public/tables/{token}")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicMenuController {

    private final GetPublicMenuUseCase getMenu;

    PublicMenuController(GetPublicMenuUseCase getMenu) {
        this.getMenu = getMenu;
    }

    @GetMapping("/menu")
    @Operation(operationId = "getPublicMenu",
            summary = "O cardápio do restaurante da mesa: categorias com produtos, preços e a foto de capa")
    @ApiResponse(responseCode = "200", description = "O cardápio")
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    PublicMenuResponse menu(@PathVariable String token) {
        return PublicMenuResponse.from(getMenu.get(token));
    }
}
