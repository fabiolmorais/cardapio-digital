package br.com.cardapio.application.service;

import br.com.cardapio.application.port.out.device.StaffShiftPort;
import br.com.cardapio.application.port.out.menu.ProductStatusHistoryPort;
import br.com.cardapio.application.port.out.order.DuplicateWarningPort;
import br.com.cardapio.application.port.out.rodizio.RodizioCardHistoryPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.ShiftEnd;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductStatusChange;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.rodizio.model.TabRodizioId;
import br.com.cardapio.domain.rodizio.model.TabRodizioStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Os dados novos dos relatórios (R5) na memória: o que cada serviço gravou. */
public final class HistoryFakes implements StaffShiftPort, ProductStatusHistoryPort, RodizioCardHistoryPort,
        DuplicateWarningPort {

    public record Shift(UserId userId, DeviceId deviceId, RefreshTokenId familyId) {
    }

    public record Ended(Object of, ShiftEnd reason) {
    }

    public record Warning(TabId tabId, UserId by, List<ProductId> products, boolean confirmed) {
    }

    public final List<Shift> shifts = new ArrayList<>();
    public final List<Ended> ended = new ArrayList<>();
    public final List<ProductStatusChange> productChanges = new ArrayList<>();
    public final List<TabRodizioStatus> cardChanges = new ArrayList<>();
    public final List<Warning> warnings = new ArrayList<>();

    @Override
    public void start(EstablishmentId establishmentId, UserId userId, DeviceId deviceId, RefreshTokenId familyId,
                      Instant now) {
        shifts.add(new Shift(userId, deviceId, familyId));
    }

    @Override
    public void endSession(RefreshTokenId familyId, ShiftEnd reason, Instant now) {
        ended.add(new Ended(familyId, reason));
    }

    @Override
    public void endDevice(DeviceId deviceId, ShiftEnd reason, Instant now) {
        ended.add(new Ended(deviceId, reason));
    }

    @Override
    public int endExpired(Instant now) {
        return 0;
    }

    @Override
    public void record(List<ProductStatusChange> changes) {
        productChanges.addAll(changes);
    }

    @Override
    public void record(EstablishmentId establishmentId, TabRodizioId tabRodizioId, TabRodizioStatus status,
                       Instant at) {
        cardChanges.add(status);
    }

    @Override
    public void warn(EstablishmentId establishmentId, TabId tabId, UserId warnedBy, Collection<ProductId> products,
                     Instant now) {
        warnings.add(new Warning(tabId, warnedBy, List.copyOf(products), false));
    }

    @Override
    public void confirm(EstablishmentId establishmentId, TabId tabId, UserId warnedBy, Collection<ProductId> products,
                        Instant now) {
        warnings.add(new Warning(tabId, warnedBy, List.copyOf(products), true));
    }
}
