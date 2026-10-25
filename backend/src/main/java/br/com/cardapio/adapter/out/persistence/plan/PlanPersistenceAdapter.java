package br.com.cardapio.adapter.out.persistence.plan;

import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.shared.model.PageQuery;
import br.com.cardapio.domain.shared.model.ResultPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class PlanPersistenceAdapter implements LoadPlansPort {

    private final PlanJpaRepository repository;

    PlanPersistenceAdapter(PlanJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ResultPage<Plan> findActive(PageQuery query) {
        Page<PlanJpaEntity> page = repository.findByActiveTrueOrderByDisplayOrderAsc(
                PageRequest.of(query.page(), query.size()));
        return new ResultPage<>(
                page.getContent().stream().map(PlanPersistenceMapper::toDomain).toList(),
                query.page(),
                query.size(),
                page.getTotalElements());
    }
}
