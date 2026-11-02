package br.com.cardapio.adapter.out.persistence.call;

import java.time.Instant;
import java.util.UUID;

/** Projeção do chamado com a mesa, o setor e o nome de quem aceitou: uma consulta só, sem N+1. */
public interface CallRow {

    UUID getId();

    UUID getEstablishmentId();

    UUID getTableId();

    UUID getSessionId();

    String getType();

    String getPaymentMethod();

    String getStatus();

    Instant getCreatedAt();

    Instant getAcceptedAt();

    UUID getAcceptedBy();

    Instant getDoneAt();

    Instant getCanceledAt();

    String getTableLabel();

    String getSectorName();

    String getAcceptedByName();
}
