package br.com.cardapio.application.port.out.report;

import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.floor.model.SectorId;
import br.com.cardapio.domain.menu.model.MenuCategoryId;
import br.com.cardapio.domain.report.model.CartSessions;
import br.com.cardapio.domain.report.model.ProductInterestRow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Os eventos do celular no cardápio (ADR 0013; REL-27, 28, 29): os eventos de [início, fim) do
 * restaurante, cruzados com os pedidos da mesma sessão. O setor é o da mesa da sessão; a categoria, a de
 * hoje do produto.
 */
public interface MenuInterestReportPort {

    /** O primeiro evento gravado do restaurante, se houver. */
    Optional<Instant> recordingSince(EstablishmentId establishmentId);

    /**
     * O que os eventos do período somam: por produto com algum evento e, nos carrinhos, as sessões que
     * puseram algo e as que não enviaram pedido nenhum. Numa leitura só dos eventos.
     */
    Found find(EstablishmentId establishmentId, Instant start, Instant end, Optional<SectorId> sector,
               Optional<MenuCategoryId> category);

    record Found(List<ProductInterestRow> products, CartSessions carts) {
    }
}
