package br.com.cardapio.application.port.in.brand;

public interface UpdateBrandUseCase {

    BrandSummary update(UpdateBrandCommand command);
}
