package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface GetStaffMenuUseCase {

    /** O cardápio para a equipe lançar pedidos: preços e opções, sem fotos e sem vídeos (ADR 0011). */
    PublicMenu get(EstablishmentId establishmentId);
}
