package br.com.cardapio.domain.order.model;

import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.tablesession.model.TableSessionId;

import java.util.Objects;

/** De onde veio o pedido: de um celular da mesa ou de uma pessoa da equipe (ADR 0011). */
public sealed interface OrderOrigin {

    record Phone(TableSessionId sessionId) implements OrderOrigin {
        public Phone {
            Objects.requireNonNull(sessionId);
        }
    }

    record Staff(UserId userId) implements OrderOrigin {
        public Staff {
            Objects.requireNonNull(userId);
        }
    }
}
