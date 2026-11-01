package br.com.cardapio.application.port.in.menu;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ProductId;

import java.util.UUID;

public interface AddProductMediaUseCase {

    /** Segundo passo: o arquivo já está no armazenamento; confere, gera os tamanhos e guarda. */
    ProductMediaSummary add(EstablishmentId establishmentId, ProductId productId, UUID uploadId);
}
