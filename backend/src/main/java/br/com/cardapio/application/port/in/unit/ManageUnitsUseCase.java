package br.com.cardapio.application.port.in.unit;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.UnitSummary;

/** A tela "Unidades" (RF31): as unidades da empresa, quantas o plano deixa ter e a unidade nova. */
public interface ManageUnitsUseCase {

    /** As unidades da empresa do restaurante de quem pede. */
    ResultPage<UnitSummary> list(EstablishmentId current, PageQuery page);

    UnitAllowance allowance(EstablishmentId current);

    /** Cria a unidade, com quem pediu como dono. */
    UnitSummary create(CreateUnitCommand command);
}
