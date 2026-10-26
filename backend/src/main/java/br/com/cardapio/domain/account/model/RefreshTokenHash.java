package br.com.cardapio.domain.account.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 do refresh token. O banco guarda só isto: quem copiar a tabela não consegue usar os tokens.
 */
public record RefreshTokenHash(String value) {

    private static final int SHA_256_HEX_LENGTH = 64;

    public RefreshTokenHash {
        Objects.requireNonNull(value, "hash do refresh token é obrigatório");
        if (value.length() != SHA_256_HEX_LENGTH) {
            throw new IllegalArgumentException("hash do refresh token deve ter 64 caracteres");
        }
    }

    public static RefreshTokenHash of(String rawToken) {
        Objects.requireNonNull(rawToken, "token é obrigatório");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return new RefreshTokenHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e); // toda JVM tem SHA-256
        }
    }
}
