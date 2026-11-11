package br.com.cardapio.domain.device.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Pedido de vínculo de um aparelho novo.
 *
 * 1. O aparelho abre o pedido e mostra o código na tela (vale por pouco tempo).
 * 2. O gerente digita o código no painel: o pedido passa a apontar para o aparelho criado.
 * 3. O aparelho, que fica perguntando, busca o token uma única vez (claimedAt).
 *
 * O segredo (secretHash) prova que quem busca o token é o mesmo aparelho que abriu o pedido:
 * só ele recebeu o valor. O código sozinho não basta, porque ele aparece na tela.
 *
 * No convite por QR Code (invite), o gerente cria o aparelho no painel e o segredo vai no QR: o
 * pedido nasce vinculado, e o aparelho que escanear busca o token direto.
 */
public record DevicePairing(
        PairingId id,
        PairingCode code,
        DeviceSecretHash secretHash,
        Instant createdAt,
        Instant expiresAt,
        DeviceId deviceId,
        Instant claimedAt,
        boolean invite
) {

    /** O pedido aberto pelo aparelho (o fluxo do código). */
    public DevicePairing(PairingId id, PairingCode code, DeviceSecretHash secretHash, Instant createdAt,
                         Instant expiresAt, DeviceId deviceId, Instant claimedAt) {
        this(id, code, secretHash, createdAt, expiresAt, deviceId, claimedAt, false);
    }

    public DevicePairing {
        Objects.requireNonNull(id);
        Objects.requireNonNull(code);
        Objects.requireNonNull(secretHash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("o pedido de vínculo precisa vencer depois de criado");
        }
    }

    public static DevicePairing start(PairingCode code, DeviceSecretHash secretHash, Instant now, Duration ttl) {
        return new DevicePairing(PairingId.newId(), code, secretHash, now, now.plus(ttl), null, null);
    }

    /**
     * O convite por QR Code: o gerente cria o aparelho no painel e o pedido já nasce apontando
     * para ele. O segredo vai no QR; quem o escanear (dentro da validade) busca o token uma vez.
     */
    public static DevicePairing invite(PairingCode code, DeviceSecretHash secretHash, DeviceId deviceId, Instant now,
                                       Duration ttl) {
        return new DevicePairing(PairingId.newId(), code, secretHash, now, now.plus(ttl),
                Objects.requireNonNull(deviceId), null, true);
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Ainda dá para o gerente vincular: dentro da validade e sem aparelho. */
    public boolean isOpen(Instant now) {
        return deviceId == null && !isExpired(now);
    }

    /**
     * Venceu para buscar o token? No fluxo do código, depois de vinculado, não vence mais: o
     * aparelho busca logo em seguida, mas pode ter ficado sem rede uns minutos. O QR do convite
     * fica à vista na tela do gerente, e por isso vence sempre.
     */
    public boolean expiredForClaim(Instant now) {
        return isExpired(now) && (invite || !isLinked());
    }

    public boolean isLinked() {
        return deviceId != null;
    }

    public boolean isClaimed() {
        return claimedAt != null;
    }

    /** Confere o segredo que o aparelho mandou com o hash guardado. */
    public boolean belongsTo(String rawSecret) {
        return rawSecret != null && !rawSecret.isBlank() && secretHash.equals(DeviceSecretHash.of(rawSecret));
    }
}
