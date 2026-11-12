package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.device.DeviceTokenCookie;
import br.com.cardapio.application.port.in.live.GetTvBoardUseCase;
import br.com.cardapio.application.port.in.live.TvBoard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * O resto do Modo TV na TV vinculada, sem login (desenho 8.1): a cozinha, os prontos, os setores e
 * a espera média. Em /api/v1/auth/device, como a fila de chamados: quem identifica a TV é o cookie.
 */
@RestController
@Tag(name = "Cozinha", description = "A fila da cozinha e os passos de cada item")
class TvBoardController {

    private final GetTvBoardUseCase board;

    TvBoardController(GetTvBoardUseCase board) {
        this.board = board;
    }

    @Schema(name = "TvBoard")
    record TvBoardResponse(
            @Schema(requiredMode = REQUIRED, description = "Na fila, em preparo e prontos, do mais antigo ao mais novo")
            List<KitchenItemResponse> kitchen,
            @Schema(requiredMode = REQUIRED, description = "Os setores ativos, na ordem do salão") List<String> sectors,
            @Schema(description = "A espera média dos chamados de hoje, em segundos; ausente se ninguém chamou")
            Long averageWaitSeconds
    ) {
        static TvBoardResponse from(TvBoard b) {
            return new TvBoardResponse(b.kitchen().stream().map(KitchenItemResponse::from).toList(), b.sectors(),
                    b.averageWaitSeconds());
        }
    }

    @GetMapping("/api/v1/auth/device/tv/board")
    @Operation(operationId = "getTvBoard", summary = "A cozinha, os prontos e a espera média na TV vinculada (sem login)")
    @ApiResponse(responseCode = "200", description = "O quadro da TV")
    @ApiResponse(responseCode = "401", description = "Aparelho não vinculado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "O aparelho não é uma TV",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TvBoardResponse get(@CookieValue(name = DeviceTokenCookie.NAME, required = false) String deviceToken) {
        return TvBoardResponse.from(board.get(deviceToken));
    }
}
