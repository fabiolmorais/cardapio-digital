package br.com.cardapio.domain.floor.exception;

import br.com.cardapio.domain.shared.exception.DomainException;

/** Setor com mesas ativas não pode ser desativado (422): as mesas ficariam sem lugar. */
public class SectorNotEmptyException extends DomainException {

    public SectorNotEmptyException(long activeTables) {
        super(activeTables == 1
                ? "Este setor tem 1 mesa ativa. Mova ou desative a mesa antes."
                : "Este setor tem " + activeTables + " mesas ativas. Mova ou desative as mesas antes.");
    }
}
