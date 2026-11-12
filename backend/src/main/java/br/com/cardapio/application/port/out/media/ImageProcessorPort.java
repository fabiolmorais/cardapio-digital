package br.com.cardapio.application.port.out.media;

/**
 * Lê uma foto enviada e gera os 3 tamanhos. É aqui que o tipo real é conferido: pelos bytes
 * do arquivo, e não pelo que o navegador disse.
 */
public interface ImageProcessorPort {

    /** InvalidImageException se o arquivo não for uma foto JPG ou PNG que dá para usar. */
    ProcessedImage process(byte[] original);

    /**
     * A logo, em PNG quadrado (com o fundo transparente preservado), nos tamanhos de LogoVariant.
     * InvalidImageException nas mesmas situações da foto.
     */
    ProcessedLogo processLogo(byte[] original);

    /**
     * O banner do rodízio, em JPG no formato largo (2:1), cortado pelo centro e reduzido até
     * BannerFile.WIDTH × BannerFile.HEIGHT. InvalidImageException se não for JPG ou PNG, ou se for
     * menor que BannerFile.MIN_WIDTH × BannerFile.MIN_HEIGHT.
     */
    byte[] processBanner(byte[] original);
}
