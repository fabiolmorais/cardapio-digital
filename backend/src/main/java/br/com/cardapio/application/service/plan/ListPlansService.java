package br.com.cardapio.application.service.plan;

import br.com.cardapio.application.port.in.plan.ListPlansUseCase;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.shared.model.PageQuery;
import br.com.cardapio.domain.shared.model.ResultPage;
import org.springframework.transaction.annotation.Transactional;

public class ListPlansService implements ListPlansUseCase {

    private final LoadPlansPort loadPlansPort;

    public ListPlansService(LoadPlansPort loadPlansPort) {
        this.loadPlansPort = loadPlansPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPage<Plan> listActivePlans(PageQuery query) {
        return loadPlansPort.findActive(query);
    }
}
