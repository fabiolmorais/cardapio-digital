package br.com.cardapio.domain.brand.model;

/**
 * Os tamanhos gerados da logo, sempre quadrados e em PNG (a logo costuma ter fundo
 * transparente, que o JPEG não tem): a pequena para o topo da página da mesa e para o display
 * impresso, a grande para telas maiores.
 */
public enum LogoVariant {
    SMALL(128),
    LARGE(512);

    private final int side;

    LogoVariant(int side) {
        this.side = side;
    }

    public int side() {
        return side;
    }

    public String fileName() {
        return "logo-" + side + ".png";
    }
}
