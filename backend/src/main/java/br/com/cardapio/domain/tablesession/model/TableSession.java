package br.com.cardapio.domain.tablesession.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.TokenHash;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A visita de um celular a uma mesa. Ela vence depois de um tempo SEM USO (cada uso empurra o
 * prazo), para um QR fotografado não valer para sempre. A trava de presença (Etapa 7) e o fim
 * quando a comanda fecha (Etapa 8) entram depois.
 *
 * @param endedAt null enquanto a sessão não foi encerrada
 */
public record TableSession(
        TableSessionId id,
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TokenHash tokenHash,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt,
        Instant endedAt
) {

    /** Duas horas sem uso: o exemplo da especificação. Vira configuração do restaurante na Etapa 7. */
    public static final Duration IDLE_LIMIT = Duration.ofHours(2);

    public TableSession {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(lastSeenAt);
        Objects.requireNonNull(expiresAt);
    }

    public static TableSession open(EstablishmentId establishmentId, DiningTableId tableId, TokenHash tokenHash,
                                    Instant now) {
        return new TableSession(TableSessionId.newId(), establishmentId, tableId, tokenHash, now, now,
                now.plus(IDLE_LIMIT), null);
    }

    public boolean isActive(Instant now) {
        return endedAt == null && now.isBefore(expiresAt);
    }

    /** Vale para esta mesa? O cookie de uma mesa não serve na mesa ao lado. */
    public boolean isFor(DiningTableId table) {
        return tableId.equals(table);
    }

    /** Mais um uso: o prazo recomeça a contar. */
    public TableSession touch(Instant now) {
        return new TableSession(id, establishmentId, tableId, tokenHash, createdAt, now, now.plus(IDLE_LIMIT),
                endedAt);
    }
}
