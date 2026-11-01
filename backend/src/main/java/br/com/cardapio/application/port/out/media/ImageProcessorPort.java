package br.com.cardapio.application.port.out.media;

/**
 * Lê uma foto enviada e gera os 3 tamanhos. É aqui que o tipo real é conferido: pelos bytes
 * do arquivo, e não pelo que o navegador disse.
 */
public interface ImageProcessorPort {

    /** InvalidImageException se o arquivo não for uma foto JPG ou PNG que dá para usar. */
    ProcessedImage process(byte[] original);
}
