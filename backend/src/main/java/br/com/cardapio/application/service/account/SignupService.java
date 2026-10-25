package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.SignupCommand;
import br.com.cardapio.application.port.in.account.SignupResult;
import br.com.cardapio.application.port.in.account.SignupUseCase;
import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.PasswordHasher;
import br.com.cardapio.application.port.out.account.SaveAccountPort;
import br.com.cardapio.domain.account.exception.EmailAlreadyRegisteredException;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.shared.model.Document;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.domain.shared.model.PhoneNumber;
import org.springframework.transaction.annotation.Transactional;

public class SignupService implements SignupUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadEstablishmentPort loadEstablishmentPort;
    private final SaveAccountPort saveAccountPort;
    private final PasswordHasher passwordHasher;

    public SignupService(LoadUserPort loadUserPort, LoadEstablishmentPort loadEstablishmentPort, SaveAccountPort saveAccountPort, PasswordHasher passwordHasher) {
        this.loadUserPort = loadUserPort;
        this.loadEstablishmentPort = loadEstablishmentPort;
        this.saveAccountPort = saveAccountPort;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public SignupResult signup(SignupCommand command) {
        Email email = new Email(command.email());
        Document document = new Document(command.document());
        PhoneNumber whatsapp = new PhoneNumber(command.whatsapp());
        PasswordPolicy.check(command.password());

        if (loadUserPort.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        Organization organization = Organization.create(
                command.establishmentName(),
                document,
                whatsapp,
                email
        );

        Establishment establishment = Establishment.create(
                organization.id(),
                command.establishmentName(),
                availableSlug(command.establishmentName())
        );

        User owner = User.create(
                command.ownerName(),
                email,
                passwordHasher.hash(command.password())
        );

        Membership membership = Membership.owner(owner.id(), establishment.id());

        saveAccountPort.saveNewAccount(organization, establishment, owner, membership);

        return new SignupResult(organization.id(), establishment.id(), owner.id(), establishment.slug().value());
    }

    private Slug availableSlug(String establishmentName) {
        Slug base = Slug.from(establishmentName);
        Slug candidate = base;
        int suffix = 2;
        while (loadEstablishmentPort.existsBySlug(candidate)) {
            candidate = base.withSuffix(suffix++);
        }
        return candidate;
    }
}
