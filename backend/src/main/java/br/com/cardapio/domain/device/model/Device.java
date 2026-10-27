package br.com.cardapio.domain.device.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.time.Instant;
import java.util.Objects;

/**
 * Aparelho autorizado por um restaurante (celular do salão, tablet da cozinha, TV).
 *
 * Nasce quando o gerente digita o código no painel, ainda sem token (tokenHash vazio). O token
 * é criado quando o próprio aparelho vem buscá-lo, porque é ele quem precisa guardar o valor:
 * o banco só guarda o hash.
 */
public record Device(
        DeviceId id,
        EstablishmentId establishmentId,
        String name,
        DeviceType type,
        DeviceSecretHash tokenHash,
        Instant createdAt,
        Instant lastSeenAt,
        Instant revokedAt
) {

    public static final int NAME_MAX = 60;

    public Device {
        Objects.requireNonNull(id);
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(createdAt);
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Dê um nome ao aparelho, como \"Celular do salão\"");
        }
        name = name.trim();
        if (name.length() > NAME_MAX) {
            throw new InvalidValueException("Nome do aparelho longo demais (máximo " + NAME_MAX + " caracteres)");
        }
    }

    /** O gerente autorizou o aparelho; o token vem depois, quando o aparelho buscar. */
    public static Device link(EstablishmentId establishmentId, String name, DeviceType type, Instant now) {
        return new Device(DeviceId.newId(), establishmentId, name, type, null, now, null, null);
    }

    /** O aparelho buscou o token: guarda o hash e marca que ele apareceu. */
    public Device issueToken(DeviceSecretHash newTokenHash, Instant now) {
        return new Device(id, establishmentId, name, type, Objects.requireNonNull(newTokenHash), createdAt, now, revokedAt);
    }

    /** O aparelho usou o token agora (abriu a tela do PIN, alguém entrou). */
    public Device seen(Instant now) {
        return new Device(id, establishmentId, name, type, tokenHash, createdAt, now, revokedAt);
    }

    /** Na TV ninguém entra com PIN: ela só mostra os chamados e pedidos (Modo TV). */
    public boolean acceptsPinLogin() {
        return type != DeviceType.TV;
    }

    /** Desvincular: o token deixa de valer. O registro fica (histórico). */
    public Device revoke(Instant now) {
        if (!isActive()) {
            return this;
        }
        return new Device(id, establishmentId, name, type, tokenHash, createdAt, lastSeenAt, now);
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
