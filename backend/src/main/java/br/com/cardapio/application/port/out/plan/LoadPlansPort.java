package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;

/**
 * Planos são dados globais do sistema (não pertencem a um restaurante),
 * por isso esta porta não recebe establishmentId.
 */
public interface LoadPlansPort {

    ResultPage<Plan> findActive(PageQuery query);
}
