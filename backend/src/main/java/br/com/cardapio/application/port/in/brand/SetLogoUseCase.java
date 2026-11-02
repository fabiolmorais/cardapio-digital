package br.com.cardapio.application.port.in.brand;

import br.com.cardapio.domain.account.model.EstablishmentId;

import java.util.UUID;

public interface SetLogoUseCase {

    /** Passo 2: o arquivo já está no armazenamento; confere, gera os tamanhos e troca a logo. */
    BrandSummary set(EstablishmentId establishmentId, UUID uploadId);
}
