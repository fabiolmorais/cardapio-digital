package br.com.cardapio.adapter.in.sse;

import br.com.cardapio.adapter.in.web.device.DeviceTokenCookie;
import br.com.cardapio.application.port.in.realtime.WatchTvEventsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** O canal de tempo real da TV vinculada: o mesmo da equipe, identificado pelo cookie do aparelho. */
@RestController
@Tag(name = "Tempo real", description = "Avisos do servidor para as telas (SSE)")
class TvEventsController {

    private final WatchTvEventsUseCase watch;
    private final SseStreams streams;

    TvEventsController(WatchTvEventsUseCase watch, SseStreams streams) {
        this.watch = watch;
        this.streams = streams;
    }

    @GetMapping(path = "/api/v1/auth/device/tv/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(operationId = "streamTvEvents",
            summary = "Avisos do restaurante na TV vinculada (sem login): \"ready\" e \"call\", como os da equipe")
    @ApiResponse(responseCode = "200", description = "Fluxo SSE; cada aviso traz só ids e status",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Aparelho não vinculado")
    @ApiResponse(responseCode = "403", description = "O aparelho não é uma TV")
    SseEmitter events(@CookieValue(name = DeviceTokenCookie.NAME, required = false) String deviceToken) {
        return streams.open((onEvent, onEnd) -> watch.watch(deviceToken, onEvent, onEnd));
    }
}
