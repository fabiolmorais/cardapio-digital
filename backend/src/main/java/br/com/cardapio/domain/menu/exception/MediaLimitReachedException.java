package br.com.cardapio.domain.menu.exception;

import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.shared.exception.DomainException;

/** 422: a galeria do produto está cheia. */
public class MediaLimitReachedException extends DomainException {

    public MediaLimitReachedException() {
        super("Cada produto pode ter até " + ProductMedia.MAX_PER_PRODUCT + " fotos. Remova uma antes de enviar outra.");
    }
}
