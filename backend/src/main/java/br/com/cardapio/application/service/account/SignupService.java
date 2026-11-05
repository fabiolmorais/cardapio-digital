package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.SignupCommand;
import br.com.cardapio.application.port.in.account.SignupResult;
import br.com.cardapio.application.port.in.account.SignupUseCase;
import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.account.SaveAccountPort;
import br.com.cardapio.application.port.out.plan.LoadPlansPort;
import br.com.cardapio.application.port.out.plan.SubscriptionPort;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.domain.account.exception.EmailAlreadyRegisteredException;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.Subscription;
import br.com.cardapio.domain.shared.model.Document;
import br.com.cardapio.domain.shared.model.Email;
import br.com.cardapio.domain.shared.model.PhoneNumber;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

public class SignupService implements SignupUseCase {

    private final LoadUserPort loadUserPort;
    private final LoadEstablishmentPort loadEstablishmentPort;
    private final SaveAccountPort saveAccountPort;
    private final PasswordHasher passwordHasher;
    private final LoadPlansPort plans;
    private final SubscriptionPort subscriptions;
    private final Clock clock;

    public SignupService(LoadUserPort loadUserPort, LoadEstablishmentPort loadEstablishmentPort, SaveAccountPort saveAccountPort, PasswordHasher passwordHasher,
                         LoadPlansPort plans, SubscriptionPort subscriptions, Clock clock) {
        this.loadUserPort = loadUserPort;
        this.loadEstablishmentPort = loadEstablishmentPort;
        this.saveAccountPort = saveAccountPort;
        this.passwordHasher = passwordHasher;
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.clock = clock;
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

        // Todo cadastro começa no teste grátis de 14 dias, com os limites do Pro.
        Plan trialPlan = plans.findActiveByCode(Subscription.TRIAL_PLAN_CODE)
                .orElseThrow(() -> new IllegalStateException("plano do teste grátis não encontrado"));
        subscriptions.save(Subscription.startTrial(organization.id(), trialPlan.id(), clock.instant()));

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
