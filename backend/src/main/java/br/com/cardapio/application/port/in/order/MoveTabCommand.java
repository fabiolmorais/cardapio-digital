package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.order.model.TabId;

import java.util.Objects;

/** Mudar a comanda para outra mesa (ADR 0012); se a mesa tem comanda aberta, as duas se juntam. */
public record MoveTabCommand(EstablishmentId establishmentId, UserId by, TabId tabId, DiningTableId toTableId) {

    public MoveTabCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(by);
        Objects.requireNonNull(tabId);
        Objects.requireNonNull(toTableId);
    }
}
