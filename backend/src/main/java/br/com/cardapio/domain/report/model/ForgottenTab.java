package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.order.model.TabId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Uma comanda que ficou aberta tempo demais (REL-61); sem fechamento, ainda está aberta. */
public record ForgottenTab(TabId tabId, String label, Instant openedAt, Optional<Instant> closedAt) {

    public ForgottenTab {
        Objects.requireNonNull(tabId, "comanda é obrigatória");
        Objects.requireNonNull(label, "mesa é obrigatória");
        Objects.requireNonNull(openedAt, "abertura é obrigatória");
        Objects.requireNonNull(closedAt, "fechamento é obrigatório");
    }
}
