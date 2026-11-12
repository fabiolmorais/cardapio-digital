package br.com.cardapio.application.port.out.live;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.order.model.TabId;
import br.com.cardapio.domain.shared.model.Money;

import java.time.Instant;

/**
 * Uma mesa ativa agora, para o mapa do salão: quantos celulares estão nela e a comanda aberta.
 *
 * @param phones        celulares com a sessão ativa
 * @param tabId         a comanda aberta; null se a mesa não tem
 * @param tabOpenedAt   desde quando a comanda está aberta; null sem comanda
 * @param tabTotal      os itens não cancelados mais o rodízio; zero sem comanda
 * @param itemsOnTheWay itens na fila, em preparo ou prontos
 * @param rodizioPeople as pessoas no rodízio; zero se a mesa não está no rodízio
 */
public record LiveTableView(DiningTableId id, String label, SectorId sectorId, String sectorName, int phones,
                            TabId tabId, Instant tabOpenedAt, Money tabTotal, int itemsOnTheWay,
                            int rodizioPeople) {
}
