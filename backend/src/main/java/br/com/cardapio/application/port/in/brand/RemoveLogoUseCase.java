package br.com.cardapio.application.port.in.brand;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface RemoveLogoUseCase {

    /** Volta para as iniciais. */
    BrandSummary remove(EstablishmentId establishmentId);
}
