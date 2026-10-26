package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.CurrentAccount;
import br.com.cardapio.application.port.in.account.GetCurrentAccountUseCase;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.domain.account.exception.AccessRevokedException;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.account.model.UserId;
import org.springframework.transaction.annotation.Transactional;

public class GetCurrentAccountService implements GetCurrentAccountUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadMembershipPort loadMembershipPort;

    public GetCurrentAccountService(LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort) {
        this.loadUserPort = loadUserPort;
        this.loadMembershipPort = loadMembershipPort;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentAccount current(UserId userId, EstablishmentId establishmentId) {
        User user = loadUserPort.findById(userId).filter(User::active)
                .orElseThrow(AccessRevokedException::new);

        ActiveMembership membership = loadMembershipPort.findActive(userId, establishmentId)
                .orElseThrow(AccessRevokedException::new);

        return new CurrentAccount(user.id(), user.name(), user.email(), Accesses.from(membership));
    }
}
