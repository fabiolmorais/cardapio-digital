package br.com.cardapio.domain.brand.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * O link "Avalie no Google". Só aceita endereços do Google: a página da mesa mostra o botão
 * para todos os clientes, e um link qualquer ali seria um convite a golpe (uma página falsa
 * pedindo o cartão, por exemplo).
 */
public record ReviewLink(String value) {

    public static final int MAX = 300;
    private static final Set<String> GOOGLE_HOSTS = Set.of(
            "g.page", "search.google.com", "www.google.com", "google.com", "maps.google.com",
            "www.google.com.br", "google.com.br", "maps.app.goo.gl", "goo.gl");
    private static final String MESSAGE = "Use o link de avaliação do Google (começa com https://g.page/ ou https://search.google.com/)";

    public ReviewLink {
        if (value == null || value.length() > MAX) {
            throw new InvalidValueException(MESSAGE);
        }
        try {
            URI uri = new URI(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equals(uri.getScheme()) || !GOOGLE_HOSTS.contains(host) || uri.getUserInfo() != null) {
                throw new InvalidValueException(MESSAGE);
            }
        } catch (URISyntaxException e) {
            throw new InvalidValueException(MESSAGE);
        }
    }

    /** Vazio vira null (sem o botão "Avalie no Google"). */
    public static ReviewLink parse(String text) {
        return text == null || text.isBlank() ? null : new ReviewLink(text.trim());
    }
}
