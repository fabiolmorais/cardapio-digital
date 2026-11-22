package br.com.cardapio.application.port.out.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.order.model.TabId;

import java.time.Instant;
import java.util.Collection;

/**
 * Os avisos de pedido repetido ao garçom (REL-50, ADR 0011): o que foi avisado e o que ele
 * confirmou. O avisado que nunca foi confirmado é o item que ele tirou (ou o envio de que desistiu).
 */
public interface DuplicateWarningPort {

    /**
     * Grava os avisos numa transação própria: o envio avisado é recusado (e desfeito), e o aviso
     * precisa ficar. Um aviso ainda sem confirmação do mesmo produto, comanda e garçom não se repete.
     */
    void warn(EstablishmentId establishmentId, TabId tabId, UserId warnedBy, Collection<ProductId> products,
              Instant now);

    /** O garçom confirmou que era outro pedido: marca os avisos dele (ou grava já confirmados, se não havia). */
    void confirm(EstablishmentId establishmentId, TabId tabId, UserId warnedBy, Collection<ProductId> products,
                 Instant now);
}
