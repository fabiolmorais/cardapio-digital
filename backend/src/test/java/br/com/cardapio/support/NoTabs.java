package br.com.cardapio.support;

import br.com.cardapio.application.port.out.order.ClosedTabView;
import br.com.cardapio.application.port.out.order.OpenTabView;
import br.com.cardapio.application.port.out.order.OrderSettingsPort;
import br.com.cardapio.application.port.out.order.TabPort;
import br.com.cardapio.application.port.out.order.TabView;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.OrderSettings;
import br.com.cardapio.domain.order.model.ServiceDestination;
import br.com.cardapio.domain.order.model.Tab;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.order.model.TabPeriod;

import java.util.List;
import java.util.Optional;

/** Nenhuma comanda e a configuração padrão: para os testes de chamados, que não dependem da conta. */
public final class NoTabs implements TabPort, OrderSettingsPort {

    @Override
    public Optional<Tab> findOpen(EstablishmentId establishmentId, DiningTableId tableId) {
        return Optional.empty();
    }

    @Override
    public boolean lockOpen(EstablishmentId establishmentId, TabId tabId) {
        return false;
    }

    @Override
    public boolean insertIfNoneOpen(Tab tab) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<TabView> findView(EstablishmentId establishmentId, TabId tabId) {
        return Optional.empty();
    }

    @Override
    public boolean close(Tab closed, ServiceDestination destination, UserId waiter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean saveService(Tab tab) {
        return false;
    }

    @Override
    public Optional<UserId> findTopAttendant(Tab tab) {
        return Optional.empty();
    }

    @Override
    public ResultPage<OpenTabView> findOpenTabs(EstablishmentId establishmentId, PageQuery page) {
        return new ResultPage<>(List.of(), page.page(), page.size(), 0);
    }

    @Override
    public ResultPage<ClosedTabView> findClosed(EstablishmentId establishmentId, TabPeriod period,
                                                DiningTableId tableId, PageQuery page) {
        return new ResultPage<>(List.of(), page.page(), page.size(), 0);
    }

    @Override
    public boolean saveMoved(Tab tab) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markMerged(EstablishmentId establishmentId, TabId tabId, TabId into) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<OrderSettings> find(EstablishmentId establishmentId) {
        return Optional.empty();
    }

    @Override
    public void save(OrderSettings settings) {
        throw new UnsupportedOperationException();
    }
}
