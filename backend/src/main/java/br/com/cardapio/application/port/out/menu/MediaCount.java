package br.com.cardapio.application.port.out.menu;

/** Quantas fotos e vídeos a galeria de um produto tem (sem os removidos nem os vídeos que falharam). */
public record MediaCount(int photos, int videos) {

    public static final MediaCount NONE = new MediaCount(0, 0);
}
