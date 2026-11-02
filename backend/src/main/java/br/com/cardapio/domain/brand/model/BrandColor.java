package br.com.cardapio.domain.brand.model;

/**
 * A paleta da marca. Cada cor traz os três tons que a página da mesa usa:
 * - primary: fundo dos botões, sempre com texto branco por cima;
 * - textOnDark e textOnLight: a cor usada como TEXTO (destaques), no tema escuro e no claro.
 *
 * Por que uma paleta, e não "qualquer cor"? Com uma cor livre, o dono escolhe um amarelo-claro,
 * e o botão fica com texto branco ilegível. Aqui cada tom já foi conferido para ter contraste
 * de pelo menos 4,5:1 (a regra de acessibilidade WCAG AA para texto).
 */
public enum BrandColor {
    TOMATO("Tomate", "#CC3F1C", "#FF8466", "#B8361A"),
    WINE("Vinho", "#9F1239", "#FB7192", "#9F1239"),
    MUSTARD("Mostarda", "#A16207", "#FACC15", "#854D0E"),
    OLIVE("Oliva", "#3F6212", "#A3E635", "#3F6212"),
    PETROL("Petróleo", "#0F766E", "#5EEAD4", "#0F766E"),
    INDIGO("Anil", "#3730A3", "#A5B4FC", "#3730A3");

    private final String label;
    private final String primary;
    private final String textOnDark;
    private final String textOnLight;

    BrandColor(String label, String primary, String textOnDark, String textOnLight) {
        this.label = label;
        this.primary = primary;
        this.textOnDark = textOnDark;
        this.textOnLight = textOnLight;
    }

    public String label() { return label; }
    public String primary() { return primary; }
    public String textOnDark() { return textOnDark; }
    public String textOnLight() { return textOnLight; }
}
