package br.com.cardapio.application.port.in.tablesession;

import br.com.cardapio.application.port.shared.PageQuery;
import br.com.cardapio.application.port.shared.ResultPage;
import br.com.cardapio.domain.account.model.EstablishmentId;

/** Os celulares bloqueados que ainda estão com a sessão aberta, do bloqueio mais recente ao mais antigo. */
public interface ListBlockedPhonesUseCase {

    ResultPage<BlockedPhone> list(EstablishmentId establishmentId, PageQuery page);
}
