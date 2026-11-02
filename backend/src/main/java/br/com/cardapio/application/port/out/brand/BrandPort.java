package br.com.cardapio.application.port.out.brand;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.brand.model.Brand;

public interface BrandPort {

    /**
     * A marca do restaurante do token. Ele sempre existe (o token só é emitido para um
     * restaurante de verdade), então não há Optional: faltar é erro de programação.
     */
    Brand find(EstablishmentId establishmentId);

    void save(Brand brand);
}
