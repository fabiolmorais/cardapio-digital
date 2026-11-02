package br.com.cardapio.adapter.in.sse;

import br.com.cardapio.adapter.in.web.shared.AuthenticatedUser;
import br.com.cardapio.application.port.in.realtime.WatchStaffEventsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** O canal de tempo real da equipe: um por restaurante, com tudo o que acontece nele. */
@RestController
@Tag(name = "Tempo real", description = "Avisos do servidor para as telas (SSE)")
@SecurityRequirement(name = "bearer")
class StaffEventsController {

    private final WatchStaffEventsUseCase watch;
    private final SseStreams streams;

    StaffEventsController(WatchStaffEventsUseCase watch, SseStreams streams) {
        this.watch = watch;
        this.streams = streams;
    }

    @GetMapping(path = "/api/v1/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'STAFF', 'KITCHEN')")
    @Operation(operationId = "streamStaffEvents",
            summary = "Avisos do restaurante em tempo real: \"ready\" ao conectar, \"call\" quando um chamado muda")
    @ApiResponse(responseCode = "200", description = "Fluxo SSE; cada aviso traz só ids e status",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")))
    SseEmitter events(@AuthenticationPrincipal Jwt jwt) {
        var establishmentId = AuthenticatedUser.from(jwt).establishmentId();
        return streams.open((onEvent, onEnd) -> watch.watch(establishmentId, onEvent, onEnd));
    }
}
