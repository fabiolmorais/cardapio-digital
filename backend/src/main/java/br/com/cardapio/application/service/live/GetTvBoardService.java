package br.com.cardapio.application.service.live;

import br.com.cardapio.application.port.in.live.GetTvBoardUseCase;
import br.com.cardapio.application.port.in.live.TvBoard;
import br.com.cardapio.application.port.in.order.KitchenItem;
import br.com.cardapio.application.port.out.floor.SectorPort;
import br.com.cardapio.application.port.out.live.LiveFloorPort;
import br.com.cardapio.application.port.out.order.OrderPort;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.service.device.DeviceAuthenticator;
import br.com.cardapio.domain.order.model.OrderItemStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;

public class GetTvBoardService implements GetTvBoardUseCase {

    private static final PageQuery ALL = new PageQuery(0, PageQuery.MAX_SIZE);

    private final DeviceAuthenticator devices;
    private final OrderPort orders;
    private final SectorPort sectors;
    private final LiveFloorPort floor;
    private final Clock clock;

    public GetTvBoardService(DeviceAuthenticator devices, OrderPort orders, SectorPort sectors, LiveFloorPort floor,
                             Clock clock) {
        this.devices = devices;
        this.orders = orders;
        this.sectors = sectors;
        this.floor = floor;
        this.clock = clock;
    }

    /** Não é readOnly: o aparelho fica marcado como "visto agora", como na fila de chamados. */
    @Override
    @Transactional
    public TvBoard get(String deviceToken) {
        var establishmentId = devices.authenticateTv(deviceToken).device().establishmentId();
        var kitchen = orders.findQueue(establishmentId,
                        EnumSet.of(OrderItemStatus.PENDING, OrderItemStatus.PREPARING, OrderItemStatus.READY), ALL)
                .content().stream().map(KitchenItem::from).toList();
        var names = sectors.findAll(establishmentId, false, ALL).content().stream()
                .map(s -> s.sector().name()).toList();
        var wait = floor.averageWaitSeconds(establishmentId, clock.instant());
        return new TvBoard(kitchen, names, wait.isPresent() ? wait.getAsLong() : null);
    }
}
