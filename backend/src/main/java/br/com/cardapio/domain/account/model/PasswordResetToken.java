package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.account.exception.PasswordResetLinkInvalidException;
import br.com.cardapio.domain.shared.model.TokenHash;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * O link de "esqueci a senha": vale 30 minutos e uma vez só. Guarda só o hash do token; o token
 * mesmo vai no e-mail e não fica em lugar nenhum.
 */
public record PasswordResetToken(
        PasswordResetTokenId id,
        UserId userId,
        TokenHash hash,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt) {

    /** O suficiente para abrir o e-mail e trocar a senha; curto o bastante para um link esquecido não servir. */
    public static final Duration VALIDITY = Duration.ofMinutes(30);

    /** Pedidos por pessoa a cada hora: o resto é ignorado em silêncio (sem encher a caixa de ninguém). */
    public static final int MAX_PER_HOUR = 3;

    public PasswordResetToken {
        Objects.requireNonNull(id);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(hash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
    }

    public static PasswordResetToken issue(UserId userId, TokenHash hash, Instant now) {
        return new PasswordResetToken(PasswordResetTokenId.newId(), userId, hash, now, now.plus(VALIDITY), null);
    }

    public boolean usable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    /** Usa o link agora. Já usado ou vencido: PasswordResetLinkInvalidException. */
    public PasswordResetToken use(Instant now) {
        if (!usable(now)) {
            throw new PasswordResetLinkInvalidException();
        }
        return new PasswordResetToken(id, userId, hash, createdAt, expiresAt, now);
    }
}
