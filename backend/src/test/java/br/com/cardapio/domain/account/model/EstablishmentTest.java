package br.com.cardapio.domain.account.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstablishmentTest {

    @Test
    void aUnitIsANewRestaurantOfTheSameCompanyWithItsTimeZone() {
        OrganizationId company = OrganizationId.newId();
        Establishment unit = Establishment.unit(company, "Sakura Manaus", Slug.from("Sakura Manaus"), "America/Manaus");

        assertThat(unit.organizationId()).isEqualTo(company);
        assertThat(unit.timeZone()).isEqualTo("America/Manaus");
        assertThat(unit.active()).isTrue();
    }

    @Test
    void theTimeZoneMustBeOneOfTheList() {
        OrganizationId company = OrganizationId.newId();
        Slug slug = Slug.from("Sakura");
        assertThatThrownBy(() -> Establishment.unit(company, "Sakura", slug, "Europe/Lisbon"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessage("Escolha um fuso horário da lista");
        assertThatThrownBy(() -> Establishment.unit(company, "Sakura", slug, null))
                .isInstanceOf(InvalidValueException.class);
    }
}
