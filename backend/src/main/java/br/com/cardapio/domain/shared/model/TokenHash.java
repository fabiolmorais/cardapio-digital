package br.com.cardapio.domain.shared.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 de um token secreto, em hexadecimal. O banco guarda só isto: quem copiar a tabela
 * não consegue usar os tokens. É a mesma ideia do RefreshTokenHash (Guia 3), agora num lugar
 * comum para os próximos tokens.
 */
public record TokenHash(String value) {

    private static final int SHA_256_HEX_LENGTH = 64;

    public TokenHash {
        Objects.requireNonNull(value, "hash do token é obrigatório");
        if (value.length() != SHA_256_HEX_LENGTH) {
            throw new IllegalArgumentException("hash do token deve ter 64 caracteres");
        }
    }

    public static TokenHash of(String rawToken) {
        Objects.requireNonNull(rawToken, "token é obrigatório");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return new TokenHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e); // toda JVM tem SHA-256
        }
    }
}
