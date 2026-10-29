package br.com.cardapio.application.port.out.floor;

import br.com.cardapio.domain.floor.model.DiningTable;

/** A mesa encontrada pelo token do QR, com o nome do restaurante dela. */
public record PublicTableView(DiningTable table, String establishmentName) {
}
