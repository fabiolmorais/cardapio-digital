package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.shared.model.PageQuery;
import br.com.cardapio.domain.shared.model.ResultPage;

/**
 * Planos são dados globais do sistema (não pertencem a um restaurante),
 * por isso esta porta não recebe establishmentId.
 */
public interface LoadPlansPort {

    ResultPage<Plan> findActive(PageQuery query);
}
