package br.com.cardapio.application.port.in.floor;

/** Um arquivo pronto para baixar. */
public record RenderedFile(String fileName, String contentType, byte[] content) {
}
