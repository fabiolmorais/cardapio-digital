package br.com.cardapio.application.port.in.brand;

import br.com.cardapio.domain.account.model.EstablishmentId;

public interface GetBrandUseCase {

    BrandSummary get(EstablishmentId establishmentId);
}
