package br.com.cardapio.adapter.in.web.menu;

import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.menu.RecordMenuEventsUseCase;
import br.com.cardapio.domain.menu.model.MenuEvent;
import br.com.cardapio.domain.menu.model.MenuEventKind;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Os eventos do cardápio que o celular da mesa manda em lote (REL-27, 28, 29): o prato aberto, o reel
 * visto e o item posto no carrinho. Aceita o corpo como texto ({@code text/plain}): é assim que o
 * {@code navigator.sendBeacon} manda sem a consulta prévia do CORS, ao sair da página. O cookie da
 * sessão continua obrigatório (SameSite=Strict: outro site não o envia).
 */
@RestController
@RequestMapping("/api/v1/public/tables/{token}/events")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicMenuEventController {

    @Schema(name = "MenuEventsRequest", description = "Os eventos do cardápio desde o último envio")
    record EventsRequest(@Schema(requiredMode = REQUIRED, maxLength = 50) List<Event> events) {
    }

    @Schema(name = "MenuEventItem", description = "Um evento: o prato e o que aconteceu com ele")
    record Event(@Schema(requiredMode = REQUIRED) MenuEventKind kind,
                 @Schema(requiredMode = REQUIRED) UUID productId,
                 @Schema(description = "Quantos foram para o carrinho (só em CART_ADD)") Integer quantity) {
    }

    private final RecordMenuEventsUseCase record;
    private final JsonMapper json;

    PublicMenuEventController(RecordMenuEventsUseCase record, JsonMapper json) {
        this.record = record;
        this.json = json;
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Operation(operationId = "recordMenuEvents", summary = "Conta o que o celular fez no cardápio (anônimo)",
            requestBody = @RequestBody(required = true, content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = EventsRequest.class))))
    @ApiResponse(responseCode = "202", description = "Recebido")
    @ApiResponse(responseCode = "400", description = "Corpo inválido ou eventos demais", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "QR inválido, trocado ou mesa desativada", content = @Content(
            mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> record(@PathVariable String token,
                                @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
                                @org.springframework.web.bind.annotation.RequestBody String body) {
        record.record(token, session, parse(body));
        return ResponseEntity.accepted().build();
    }

    private List<MenuEvent> parse(String body) {
        EventsRequest request;
        try {
            request = json.readValue(body, EventsRequest.class);
        } catch (JacksonException e) {
            throw new InvalidValueException("Eventos do cardápio em formato inválido");
        }
        if (request == null || request.events() == null || request.events().size() > MenuEvent.MAX_BATCH) {
            throw new InvalidValueException("Eventos do cardápio em formato inválido");
        }
        return request.events().stream().map(e -> {
            if (e == null || e.kind() == null || e.productId() == null) {
                throw new InvalidValueException("Eventos do cardápio em formato inválido");
            }
            return new MenuEvent(e.kind(), new ProductId(e.productId()), e.quantity() == null ? 1 : e.quantity());
        }).toList();
    }
}
