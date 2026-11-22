package br.com.cardapio.application.port.out.menu;

import br.com.cardapio.domain.menu.model.ProductStatusChange;

import java.util.List;

/** O histórico de promoção, destaque e "disponível" dos produtos (REL-22, 24, 25). */
public interface ProductStatusHistoryPort {

    void record(List<ProductStatusChange> changes);
}
