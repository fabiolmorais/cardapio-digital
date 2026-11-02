package br.com.cardapio.domain.brand.model;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.shared.exception.InvalidValueException;

import java.util.Objects;
import java.util.UUID;

/**
 * A marca do restaurante: o que o cliente vê na página da mesa e no display impresso.
 * Mora na mesma linha do estabelecimento; é outro "olhar" sobre ele, com as regras de aparência.
 *
 * @param logoPath    pasta da logo no armazenamento; null quando a página mostra as iniciais
 * @param instagram   null quando o restaurante não informou
 * @param reviewLink  null quando o restaurante não informou
 */
public record Brand(
        EstablishmentId establishmentId,
        String name,
        BrandColor color,
        Theme theme,
        String logoPath,
        InstagramHandle instagram,
        ReviewLink reviewLink
) {

    public static final int NAME_MAX = 120;

    public Brand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(color);
        Objects.requireNonNull(theme);
        name = validName(name);
    }

    /** O formulário "Marca e aparência": troca tudo, menos a logo (que tem o próprio envio). */
    public Brand edit(String newName, BrandColor newColor, Theme newTheme, InstagramHandle newInstagram,
                      ReviewLink newReviewLink) {
        return new Brand(establishmentId, newName, newColor, newTheme, logoPath, newInstagram, newReviewLink);
    }

    /**
     * Logo nova, numa pasta nova (a antiga fica, como as fotos). A pasta leva o restaurante, para
     * achar tudo dele pelo começo do caminho.
     */
    public Brand withNewLogo() {
        String path = "establishments/" + establishmentId.value() + "/logo/" + UUID.randomUUID();
        return new Brand(establishmentId, name, color, theme, path, instagram, reviewLink);
    }

    /** Volta para as iniciais. */
    public Brand withoutLogo() {
        return new Brand(establishmentId, name, color, theme, null, instagram, reviewLink);
    }

    public boolean hasLogo() {
        return logoPath != null;
    }

    public String logoFile(LogoVariant variant) {
        if (!hasLogo()) {
            throw new IllegalStateException("o restaurante não tem logo");
        }
        return logoPath + "/" + variant.fileName();
    }

    /** O mesmo limite do cadastro (Guia 1): um nome que já foi aceito nunca deixa a tela de abrir. */
    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Informe o nome do restaurante");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX) {
            throw new InvalidValueException("Nome longo demais (máximo " + NAME_MAX + " caracteres)");
        }
        return trimmed;
    }
}
