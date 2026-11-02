package br.com.cardapio.adapter.in.sse;

import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.realtime.WatchTableEventsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** O canal de tempo real da mesa: só os avisos daquela mesa, para o celular com a sessão dela. */
@RestController
@Tag(name = "Tempo real", description = "Avisos do servidor para as telas (SSE)")
class TableEventsController {

    private final WatchTableEventsUseCase watch;
    private final SseStreams streams;

    TableEventsController(WatchTableEventsUseCase watch, SseStreams streams) {
        this.watch = watch;
        this.streams = streams;
    }

    @GetMapping(path = "/api/v1/public/tables/{token}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(operationId = "streamTableEvents",
            summary = "Avisos da mesa em tempo real: \"ready\" ao conectar, \"call\" quando um chamado dela muda")
    @ApiResponse(responseCode = "200", description = "Fluxo SSE; cada aviso traz só ids e status",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa")
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada")
    SseEmitter events(@PathVariable String token,
                      @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        return streams.open((onEvent, onEnd) -> watch.watch(token, session, onEvent, onEnd));
    }
}
