package br.com.cardapio.application.port.out.floor;

import java.util.List;

/**
 * Desenha o QR Code e o display das mesas. Como gerar o QR e o PDF é detalhe de
 * infraestrutura (bibliotecas), fica atrás desta porta.
 */
public interface TableDisplayRenderer {

    /** Só o QR Code, em PNG grande, para quem vai montar a própria arte. */
    byte[] qrPng(String url);

    /** Só o QR Code, em SVG (vetor: amplia sem perder qualidade). */
    byte[] qrSvg(String url);

    /** O display pronto para imprimir, uma página por mesa. */
    byte[] displaysPdf(List<DisplayContent> displays);
}
