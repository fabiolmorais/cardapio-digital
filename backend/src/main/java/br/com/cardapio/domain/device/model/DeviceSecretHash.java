package br.com.cardapio.domain.device.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 de um segredo do aparelho (o token dele e o segredo do pedido de vínculo).
 * Mesma ideia do RefreshTokenHash: o banco nunca guarda o valor, só o hash.
 */
public record DeviceSecretHash(String value) {

    private static final int SHA_256_HEX_LENGTH = 64;

    public DeviceSecretHash {
        Objects.requireNonNull(value, "hash é obrigatório");
        if (value.length() != SHA_256_HEX_LENGTH) {
            throw new IllegalArgumentException("hash deve ter 64 caracteres");
        }
    }

    public static DeviceSecretHash of(String rawSecret) {
        Objects.requireNonNull(rawSecret, "segredo é obrigatório");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            return new DeviceSecretHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
