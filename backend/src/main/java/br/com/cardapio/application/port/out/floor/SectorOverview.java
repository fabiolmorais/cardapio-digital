package br.com.cardapio.application.port.out.floor;

import br.com.cardapio.domain.floor.model.Sector;

/** O setor e quantas mesas ativas ele tem (a lista mostra "Salão principal · 12"). */
public record SectorOverview(Sector sector, long activeTables) {
}
