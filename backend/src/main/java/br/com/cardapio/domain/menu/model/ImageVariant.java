package br.com.cardapio.domain.menu.model;

import java.util.Locale;

/**
 * Os 3 tamanhos gerados de cada foto. O celular baixa só o que vai mostrar: a miniatura na
 * lista, o médio no cardápio e o grande na tela do prato.
 *
 * @param maxSide maior lado, em pixels (a foto nunca é aumentada)
 * @param square  recortada em quadrado, pelo centro
 */
public enum ImageVariant {
    THUMB(240, true),
    MEDIUM(720, false),
    LARGE(1440, false);

    private final int maxSide;
    private final boolean square;

    ImageVariant(int maxSide, boolean square) {
        this.maxSide = maxSide;
        this.square = square;
    }

    public int maxSide() {
        return maxSide;
    }

    public boolean square() {
        return square;
    }

    /** Nome do arquivo dentro da pasta da foto: "thumb.jpg", "medium.jpg", "large.jpg". */
    public String fileName() {
        return name().toLowerCase(Locale.ROOT) + ".jpg";
    }
}
