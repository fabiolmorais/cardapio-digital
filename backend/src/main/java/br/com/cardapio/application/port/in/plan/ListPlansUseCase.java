package br.com.cardapio.application.port.in.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.shared.model.PageQuery;
import br.com.cardapio.domain.shared.model.ResultPage;

/**
 * Lista os planos oferecidos, na ordem de exibição. É público: não depende de restaurante.
 */
public interface ListPlansUseCase {

    ResultPage<Plan> listActivePlans(PageQuery query);
}
