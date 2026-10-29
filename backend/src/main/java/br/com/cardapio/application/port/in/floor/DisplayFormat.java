package br.com.cardapio.application.port.in.floor;

/** PNG e SVG: só o QR Code. PDF: o display pronto (restaurante, QR, chamada e número da mesa). */
public enum DisplayFormat {
    PNG("image/png", "png"),
    SVG("image/svg+xml", "svg"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extension;

    DisplayFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }
}
