package br.com.cardapio.domain.account.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Tentativas erradas de PIN de uma pessoa. Um PIN tem só 10 mil combinações (4 dígitos):
 * sem limite, dá para tentar todas em minutos. Com 5 erros a pessoa fica bloqueada por 15
 * minutos, e acertar zera a contagem.
 */
public record PinLockout(
        MembershipId membershipId,
        EstablishmentId establishmentId,
        int failedAttempts,
        Instant lockedUntil
) {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public PinLockout {
        Objects.requireNonNull(membershipId);
        Objects.requireNonNull(establishmentId);
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("tentativas não podem ser negativas");
        }
    }

    /** Ninguém errou ainda. */
    public static PinLockout clean(MembershipId membershipId, EstablishmentId establishmentId) {
        return new PinLockout(membershipId, establishmentId, 0, null);
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Mais um erro. Se um bloqueio antigo já venceu, a contagem recomeça. O quinto erro
     * seguido bloqueia.
     */
    public PinLockout registerFailure(Instant now) {
        int previous = lockedUntil != null && !isLocked(now) ? 0 : failedAttempts;
        int attempts = previous + 1;
        Instant until = attempts >= MAX_ATTEMPTS ? now.plus(LOCK_DURATION) : null;
        return new PinLockout(membershipId, establishmentId, attempts, until);
    }

    public PinLockout reset() {
        return clean(membershipId, establishmentId);
    }

    public boolean hasFailures() {
        return failedAttempts > 0;
    }

    public int remainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS - failedAttempts);
    }

    /** Minutos que faltam para desbloquear, arredondados para cima (nunca "0 minutos"). */
    public long minutesLeft(Instant now) {
        if (!isLocked(now)) {
            return 0;
        }
        long seconds = Duration.between(now, lockedUntil).toSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }
}
