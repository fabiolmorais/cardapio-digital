package br.com.cardapio.application.port.out.plan;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.plan.model.Plan;

import java.util.Optional;

/**
 * Planos são dados globais do sistema (não pertencem a um restaurante),
 * por isso esta porta não recebe establishmentId.
 */
public interface LoadPlansPort {

    ResultPage<Plan> findActive(PageQuery query);

    /** O plano ativo com este código (ex.: "pro", o do teste grátis). */
    Optional<Plan> findActiveByCode(String code);
}
