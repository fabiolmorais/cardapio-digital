package br.com.cardapio.application.port.in.order;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.DiningTableId;

import java.util.Objects;

/**
 * Abrir a comanda de um cliente do balcão: o restaurante (do token), o balcão, o nome e o cartão
 * (opcional).
 */
public record OpenCounterTabCommand(EstablishmentId establishmentId, DiningTableId counterId, String name,
                                    String card) {

    public OpenCounterTabCommand {
        Objects.requireNonNull(establishmentId);
        Objects.requireNonNull(counterId);
    }
}
