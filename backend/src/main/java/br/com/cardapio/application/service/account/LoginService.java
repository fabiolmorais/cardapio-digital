package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.LoginCommand;
import br.com.cardapio.application.port.in.account.LoginResult;
import br.com.cardapio.application.port.in.account.LoginUseCase;
import br.com.cardapio.application.port.out.account.ActiveMembership;
import br.com.cardapio.application.port.out.account.LoadMembershipPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.security.AccessToken;
import br.com.cardapio.application.port.out.security.AccessTokenIssuer;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.security.TokenSubject;
import br.com.cardapio.domain.account.exception.InvalidCredentialsException;
import br.com.cardapio.domain.account.model.PasswordHash;
import br.com.cardapio.domain.account.model.User;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Email;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LoginService implements LoginUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadMembershipPort loadMembershipPort;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer tokenIssuer;
    /** Hash de uma senha que ninguém tem: usado para gastar o mesmo tempo quando o e-mail não existe. */
    private final PasswordHash dummyHash;

    public LoginService(LoadUserPort loadUserPort, LoadMembershipPort loadMembershipPort, PasswordHasher passwordHasher, AccessTokenIssuer tokenIssuer) {
        this.loadUserPort = loadUserPort;
        this.loadMembershipPort = loadMembershipPort;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        Optional<User> found = findUser(command.email());

        String password = command.password() == null ? "" : command.password();
        if (found.isEmpty()) {
            passwordHasher.matches(password, dummyHash); // mesmo tempo de resposta: não revela se o e-mail existe
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!passwordHasher.matches(password, user.passwordHash()) || !user.active()) {
            throw new InvalidCredentialsException();
        }

        List<ActiveMembership> memberships = loadMembershipPort.findActiveByUser(user.id());
        if (memberships.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        // Por enquanto, entra no primeiro estabelecimento. A troca de estabelecimento vem com as redes (fase 3).
        ActiveMembership membership = memberships.getFirst();

        AccessToken token = tokenIssuer.issue(
                new TokenSubject(user.id(), membership.establishmentId(), membership.role()));
        return new LoginResult(token.value(), token.expiresAt(), user.id(), user.name(), Accesses.from(membership));
    }

    /** E-mail com formato inválido é tratado como "não existe": o login nunca devolve 400 por isso. */
    private Optional<User> findUser(String email) {
        try {
            return loadUserPort.findByEmail(new Email(email));
        } catch (InvalidValueException e) {
            return Optional.empty();
        }
    }
}
