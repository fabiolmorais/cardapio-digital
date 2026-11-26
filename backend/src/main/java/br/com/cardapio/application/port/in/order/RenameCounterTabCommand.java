package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.order.model.TabId;

import java.util.Objects;

/** Corrigir o nome ou o cartão de uma comanda aberta do balcão. */
public record RenameCounterTabCommand(EstablishmentId establishmentId, TabId tabId, String name, String card) {

    public RenameCounterTabCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tabId);
    }
}
