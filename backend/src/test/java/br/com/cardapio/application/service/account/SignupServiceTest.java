package br.com.cardapio.application.service.account;

import br.com.cardapio.application.port.in.account.SignupCommand;
import br.com.cardapio.application.port.in.account.SignupResult;
import br.com.cardapio.application.port.out.account.LoadEstablishmentPort;
import br.com.cardapio.application.port.out.account.LoadUserPort;
import br.com.cardapio.application.port.out.security.PasswordHasher;
import br.com.cardapio.application.port.out.account.SaveAccountPort;
import br.com.cardapio.domain.account.exception.EmailAlreadyRegisteredException;
import br.com.cardapio.domain.account.model.*;
import br.com.cardapio.domain.shared.exception.InvalidValueException;
import br.com.cardapio.domain.shared.model.Email;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste do caso de uso sem Spring e sem banco: as portas de saída são substituídas por implementações em memória.
 */
class SignupServiceTest {

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final SignupService service = new SignupService(accounts, accounts, accounts, new FakeHasher());

    private static SignupCommand command(String email, String establishmentName) {
        return new SignupCommand(establishmentName, "Marina Tanaka", "11.222.333/0001-81",
                "(11) 98765-4321", email, "sushi2026bom");
    }

    @Test
    void createsOrganizationEstablishmentOwnerAndMembership() {
        SignupResult result = service.signup(command("Marina@Sakura.com", "Sakura Sushi"));

        assertThat(result.slug()).isEqualTo("sakura-sushi");
        assertThat(accounts.users).singleElement().satisfies(u -> {
            assertThat(u.email().value()).isEqualTo("marina@sakura.com");
            assertThat(u.passwordHash().value()).isEqualTo("hash:sushi2026bom");
        });
        assertThat(accounts.memberships).singleElement().satisfies(m -> {
            assertThat(m.role()).isEqualTo(Role.OWNER);
            assertThat(m.userId()).isEqualTo(result.userId());
            assertThat(m.establishmentId()).isEqualTo(result.establishmentId());
        });
    }

    @Test
    void rejectsEmailAlreadyRegistered() {
        service.signup(command("marina@sakura.com", "Sakura Sushi"));

        assertThatThrownBy(() -> service.signup(command("MARINA@sakura.com", "Outro Restaurante")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(accounts.organizations).hasSize(1);
    }

    @Test
    void generatesUniqueSlugForRepeatedNames() {
        service.signup(command("a@sakura.com", "Sakura Sushi"));
        SignupResult second = service.signup(command("b@sakura.com", "Sakura Sushi"));

        assertThat(second.slug()).isEqualTo("sakura-sushi-2");
    }

    @Test
    void validatesBeforeSavingAnything() {
        SignupCommand invalid = new SignupCommand("Sakura Sushi", "Marina", "000.000.000-00",
                "(11) 98765-4321", "marina@sakura.com", "sushi2026bom");

        assertThatThrownBy(() -> service.signup(invalid)).isInstanceOf(InvalidValueException.class);
        assertThat(accounts.organizations).isEmpty();
    }


    private static final class FakeHasher implements PasswordHasher {
        @Override
        public PasswordHash hash(String rawPassword) {
            return new PasswordHash("hash:" + rawPassword);
        }

        @Override
        public boolean matches(String rawPassword, PasswordHash hash) {
            return hash.value().equals("hash:" + rawPassword);
        }
    }

    private static final class InMemoryAccounts implements LoadUserPort, LoadEstablishmentPort, SaveAccountPort {
        final List<Organization> organizations = new ArrayList<>();
        final List<User> users = new ArrayList<>();
        final List<Membership> memberships = new ArrayList<>();
        final Set<Slug> slugs = new HashSet<>();

        @Override
        public boolean existsByEmail(Email email) {
            return users.stream().anyMatch(u -> u.email().equals(email));
        }

        @Override
        public boolean existsBySlug(Slug slug) {
            return slugs.contains(slug);
        }

        @Override
        public void saveNewAccount(Organization organization, Establishment establishment, User owner,
                                   Membership membership) {
            organizations.add(organization);
            slugs.add(establishment.slug());
            users.add(owner);
            memberships.add(membership);
        }
    }
}