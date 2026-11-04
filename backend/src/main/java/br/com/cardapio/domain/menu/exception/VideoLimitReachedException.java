package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.shared.exception.ConflictException;

/** 409: o prato já tem os vídeos que pode ter. */
public class VideoLimitReachedException extends ConflictException {

    public VideoLimitReachedException() {
        super("Cada produto pode ter até " + ProductMedia.MAX_VIDEOS_PER_PRODUCT
                + " vídeos. Remova um antes de enviar outro.");
    }
}
