package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.EstablishmentAccess;
import br.com.cardapio.application.port.out.account.ActiveMembership;

/** Converte o dado da porta de saída no resultado da porta de entrada. */
final class Accesses {

    private Accesses() {
    }

    static EstablishmentAccess from(ActiveMembership m) {
        return new EstablishmentAccess(m.establishmentId(), m.establishmentName(), m.establishmentSlug(), m.role());
    }
}
