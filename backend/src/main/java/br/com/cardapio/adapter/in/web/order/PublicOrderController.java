package br.com.cardapio.adapter.in.web.order;

import br.com.cardapio.adapter.in.web.tablesession.TableSessionCookie;
import br.com.cardapio.application.port.in.order.CancelOrderItemUseCase;
import br.com.cardapio.application.port.in.order.ListTableOrdersUseCase;
import br.com.cardapio.application.port.in.order.PlaceOrderCommand;
import br.com.cardapio.application.port.in.order.PlaceOrderUseCase;
import br.com.cardapio.domain.order.model.OrderItemId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** O pedido pelo celular: o carrinho vira pedido, e a mesa acompanha a comanda. Sem login, com o cookie da sessão. */
@RestController
@RequestMapping("/api/v1/public/tables/{token}")
@Tag(name = "Mesa", description = "O que o cliente vê ao escanear o QR Code")
class PublicOrderController {

    private final PlaceOrderUseCase placeOrder;
    private final ListTableOrdersUseCase listOrders;
    private final CancelOrderItemUseCase cancelItem;

    PublicOrderController(PlaceOrderUseCase placeOrder, ListTableOrdersUseCase listOrders,
                          CancelOrderItemUseCase cancelItem) {
        this.placeOrder = placeOrder;
        this.listOrders = listOrders;
        this.cancelItem = cancelItem;
    }

    @GetMapping("/orders")
    @Operation(operationId = "listTableOrders",
            summary = "Os pedidos da comanda aberta da mesa (de todos os celulares) e o total parcial")
    @ApiResponse(responseCode = "200", description = "A comanda (vazia, se a mesa ainda não pediu)")
    @ApiResponse(responseCode = "401", description = "Sem sessão válida nesta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    TableOrdersResponse list(@PathVariable String token,
                             @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        return TableOrdersResponse.from(listOrders.list(token, session));
    }

    @PostMapping("/orders")
    @Operation(operationId = "placeOrder", summary = "Envia o carrinho: abre a comanda no primeiro pedido da mesa")
    @ApiResponse(responseCode = "201", description = "Pedido feito (direto para a cozinha ou parado na confirmação)")
    @ApiResponse(responseCode = "403", description = "Sem presença confirmada, ou celular bloqueado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Pedido desligado, ou um item esgotou ou saiu do cardápio",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "Pedidos demais deste celular em pouco tempo",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<TableOrderResponse> place(@PathVariable String token,
                                             @CookieValue(name = TableSessionCookie.NAME, required = false) String session,
                                             @Valid @RequestBody PlaceOrderRequest body) {
        var order = placeOrder.place(new PlaceOrderCommand(token, session, body.toLines()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TableOrderResponse.from(order));
    }

    @DeleteMapping("/order-items/{itemId}")
    @Operation(operationId = "cancelOrderItem",
            summary = "O cliente cancela um item que ele pediu, enquanto está na fila")
    @ApiResponse(responseCode = "204", description = "Cancelado")
    @ApiResponse(responseCode = "404", description = "Não é um item desta mesa",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A cozinha já começou, ou o item é de outro celular",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> cancel(@PathVariable String token, @PathVariable UUID itemId,
                                @CookieValue(name = TableSessionCookie.NAME, required = false) String session) {
        cancelItem.cancel(token, session, new OrderItemId(itemId));
        return ResponseEntity.noContent().build();
    }
}
