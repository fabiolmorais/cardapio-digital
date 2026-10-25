package br.com.cardapio.application.port.out.account;

import br.com.cardapio.domain.account.model.Establishment;
import br.com.cardapio.domain.account.model.Membership;
import br.com.cardapio.domain.account.model.Organization;
import br.com.cardapio.domain.account.model.User;

public interface SaveAccountPort {

    void saveNewAccount(
            Organization organization,
            Establishment establishment,
            User owner,
            Membership membership
    );
}
