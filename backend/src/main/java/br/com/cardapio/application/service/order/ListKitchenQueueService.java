package br.com.cardapio.application.service.order;

import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.application.port.in.order.ListKitchenQueueUseCase;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ListKitchenQueueService implements ListKitchenQueueUseCase {

    /** O que a fila mostra: o que ainda não chegou à mesa. Entregue e cancelado saem dela. */
    private static final Set<OrderItemStatus> ON_THE_WAY =
            EnumSet.of(OrderItemStatus.PENDING, OrderItemStatus.PREPARING, OrderItemStatus.READY);

    private final OrderPort orders;
    private final ProductMediaPort media;
    private final MediaStoragePort storage;

    public ListKitchenQueueService(OrderPort orders, ProductMediaPort media, MediaStoragePort storage) {
        this.orders = orders;
        this.media = media;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<KitchenItem> list(EstablishmentId establishmentId, Set<OrderItemStatus> statuses,
                                        PageQuery page) {
        if (!ON_THE_WAY.containsAll(statuses)) {
            throw new InvalidValueException("A fila só tem itens na fila, em preparo ou prontos.");
        }
        Set<OrderItemStatus> wanted = statuses.isEmpty() ? ON_THE_WAY : statuses;
        var queue = orders.findQueue(establishmentId, wanted, page);
        if (queue.content().isEmpty()) {
            return queue.map(KitchenItem::from);
        }
        Map<ProductId, String> covers = covers(establishmentId);
        return queue.map(view -> KitchenItem.from(view, covers::get));
    }

    /** A miniatura da primeira foto de cada produto (a capa, como no cardápio), numa consulta só. */
    private Map<ProductId, String> covers(EstablishmentId establishmentId) {
        Map<ProductId, String> covers = new HashMap<>();
        for (ProductMedia m : media.findReadyGalleries(establishmentId)) {
            if (!m.isVideo()) {
                covers.putIfAbsent(m.productId(), storage.publicUrl(m.fileOf(ImageVariant.THUMB)));
            }
        }
        return covers;
    }
}
