package br.com.cardapio.adapter.out.persistence.order;

import java.time.Instant;
import java.util.UUID;

/** Uma linha do histórico, lida por SQL nativo (interface: o Spring Data preenche pelos apelidos). */
public interface ClosedTabRow {

    UUID getId();

    String getTableLabel();

    String getSectorName();

    Instant getOpenedAt();

    Instant getClosedAt();

    long getTotalCents();

    String getPaymentMethod();

    String getClosedByName();
}
