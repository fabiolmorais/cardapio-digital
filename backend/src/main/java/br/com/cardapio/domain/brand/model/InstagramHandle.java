package br.com.cardapio.domain.brand.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/**
 * O @ do Instagram, guardado sem o "@" e sem o endereço: "sakurasushi.sp". A página da mesa
 * monta o link (https://instagram.com/…), e o dono pode colar o @ ou o link inteiro.
 */
public record InstagramHandle(String value) {

    /** As regras do próprio Instagram: até 30 letras, números, ponto e sublinhado. */
    private static final String PATTERN = "[A-Za-z0-9._]{1,30}";

    public InstagramHandle {
        if (value == null || !value.matches(PATTERN)) {
            throw new InvalidValueException("Instagram inválido. Use só o @, como sakurasushi.sp");
        }
    }

    /** Aceita "@sakura", "sakura" e "https://www.instagram.com/sakura/". Vazio vira null (sem Instagram). */
    public static InstagramHandle parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String handle = text.trim()
                .replaceFirst("^(https?://)?(www\\.)?instagram\\.com/", "")
                .replaceFirst("^@", "")
                .replaceFirst("/.*$", "");
        return new InstagramHandle(handle);
    }
}
