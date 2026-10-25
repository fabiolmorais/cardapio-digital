package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Nome curto e único do estabelecimento para URLs (ex.: "sakura-sushi").
 */
public record Slug(String value) {

    private static final int MAX = 60;

    public Slug {
        if (value == null || !value.matches("[a-z0-9]+(-[a-z0-9]+)*") || value.length() > MAX + 4) {
            throw new InvalidValueException("Identificador do estabelecimento inválido");
        }
    }

    /** "Sakura Sushi — Liberdade" vira "sakura-sushi-liberdade". */
    public static Slug from(String name) {
        if (name == null) {
            throw new InvalidValueException("Informe o nome do estabelecimento");
        }
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX) {
            slug = slug.substring(0, MAX).replaceAll("-+$", "");
        }
        if (slug.isEmpty()) {
            throw new InvalidValueException("O nome do estabelecimento precisa ter letras ou números");
        }
        return new Slug(slug);
    }

    /** Para nomes repetidos: "sakura-sushi-2", "sakura-sushi-3"… */
    public Slug withSuffix(int number) {
        return new Slug(value + "-" + number);
    }
}
