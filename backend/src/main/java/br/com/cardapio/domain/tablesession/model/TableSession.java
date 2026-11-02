package br.com.cardapio.domain.tablesession.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.TokenHash;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A visita de um celular a uma mesa. Ela vence depois de um tempo SEM USO (cada uso empurra o
 * prazo), para um QR fotografado não valer para sempre. Para chamar e pedir, ela precisa da
 * presença confirmada no salão (Wi-Fi, GPS ou garçom), que vale enquanto ela durar. O fim quando
 * a comanda fecha entra na Etapa 8.
 *
 * @param endedAt             null enquanto a sessão não foi encerrada
 * @param presenceMethod      null enquanto a presença não foi confirmada
 * @param presenceConfirmedAt quando foi confirmada; null junto com o presenceMethod
 * @param unlockedBy          quem da equipe liberou; só na liberação pelo garçom (STAFF)
 */
public record TableSession(
        TableSessionId id,
        EstablishmentId establishmentId,
        DiningTableId tableId,
        TokenHash tokenHash,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt,
        Instant endedAt,
        PresenceMethod presenceMethod,
        Instant presenceConfirmedAt,
        UserId unlockedBy
) {

    /** Duas horas sem uso: o exemplo da especificação. Vira configuração do restaurante no Guia 17. */
    public static final Duration IDLE_LIMIT = Duration.ofHours(2);

    public TableSession {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(tableId);
        Objects.requireNonNull(tokenHash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(lastSeenAt);
        Objects.requireNonNull(expiresAt);
        if ((presenceMethod == null) != (presenceConfirmedAt == null)) {
            throw new InvalidValueException("Presença sem método ou sem data");
        }
        if ((presenceMethod == PresenceMethod.STAFF) != (unlockedBy != null)) {
            throw new InvalidValueException("Só a liberação pelo garçom diz quem liberou");
        }
    }

    public static TableSession open(EstablishmentId establishmentId, DiningTableId tableId, TokenHash tokenHash,
                                    Instant now) {
        return new TableSession(TableSessionId.newId(), establishmentId, tableId, tokenHash, now, now,
                now.plus(IDLE_LIMIT), null, null, null, null);
    }

    public boolean isActive(Instant now) {
        return endedAt == null && now.isBefore(expiresAt);
    }

    /** Vale para esta mesa? O cookie de uma mesa não serve na mesa ao lado. */
    public boolean isFor(DiningTableId table) {
        return tableId.equals(table);
    }

    /** Já provou que está no salão? Só assim chama o garçom e pede a conta. */
    public boolean isPresent() {
        return presenceMethod != null;
    }

    /** Mais um uso: o prazo recomeça a contar. */
    public TableSession touch(Instant now) {
        return new TableSession(id, establishmentId, tableId, tokenHash, createdAt, now, now.plus(IDLE_LIMIT),
                endedAt, presenceMethod, presenceConfirmedAt, unlockedBy);
    }

    /**
     * O celular provou a presença sozinho (Wi-Fi ou GPS). Se já estava confirmada, fica a
     * primeira: é ela que conta a história da visita.
     */
    public TableSession confirmPresence(PresenceMethod method, Instant now) {
        if (method == PresenceMethod.STAFF) {
            throw new IllegalArgumentException("A liberação pelo garçom usa o unlock, com quem liberou");
        }
        if (isPresent()) {
            return this;
        }
        return new TableSession(id, establishmentId, tableId, tokenHash, createdAt, lastSeenAt, expiresAt, endedAt,
                Objects.requireNonNull(method), now, null);
    }

    /** O garçom liberou a mesa ("Liberar esta mesa"). */
    public TableSession unlock(UserId by, Instant now) {
        if (isPresent()) {
            return this;
        }
        return new TableSession(id, establishmentId, tableId, tokenHash, createdAt, lastSeenAt, expiresAt, endedAt,
                PresenceMethod.STAFF, now, Objects.requireNonNull(by));
    }
}
