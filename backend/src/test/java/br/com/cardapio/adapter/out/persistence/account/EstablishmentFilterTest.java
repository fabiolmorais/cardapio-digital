package br.com.cardapio.adapter.out.persistence.account;

import br.com.cardapio.adapter.out.persistence.shared.EstablishmentFilter;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O filtro do Hibernate corta as consultas pelo restaurante mesmo quando a consulta não tem
 * "WHERE establishment_id" (como o findAll). É a rede de proteção para um esquecimento.
 */
@IntegrationTest
class EstablishmentFilterTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    EstablishmentFilter filter;

    @Autowired
    MembershipJpaRepository memberships;

    private UUID sakura;
    private UUID otherRestaurant;

    @BeforeEach
    void twoRestaurants() throws Exception {
        sakura = signup();
        otherRestaurant = signup();
    }

    private UUID signup() throws Exception {
        String body = """
                {"establishmentName":"Restaurante %s","ownerName":"Marina","document":"11.222.333/0001-81",
                 "whatsapp":"(11) 98765-4321","email":"dono-%s@teste.com","password":"sushi2026bom","acceptedTerms":true}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        MvcTestResult result = mvc.post().uri("/api/v1/signup").contentType(MediaType.APPLICATION_JSON)
                .content(body).exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.establishmentId"));
    }

    private UUID membershipOf(UUID establishmentId) {
        return jdbc.queryForObject("SELECT id FROM membership WHERE establishment_id = ?", UUID.class, establishmentId);
    }

    @Test
    void withTheFilterOnlyTheRestaurantRowsAreVisible() {
        UUID otherMembership = membershipOf(otherRestaurant);

        transaction.executeWithoutResult(status -> {
            filter.enable(new EstablishmentId(sakura));

            // findAll não tem WHERE nenhum: quem corta é o filtro.
            assertThat(memberships.findAll())
                    .isNotEmpty()
                    .allSatisfy(m -> assertThat(m.getEstablishmentId()).isEqualTo(sakura));
            // Com applyToLoadByKey, nem a busca direta por id enxerga o outro restaurante.
            assertThat(memberships.findById(otherMembership)).isEmpty();
        });
    }

    @Test
    void withoutTheFilterEverythingIsVisible() {
        transaction.executeWithoutResult(status ->
                assertThat(memberships.findAll())
                        .extracting(MembershipJpaEntity::getEstablishmentId)
                        .contains(sakura, otherRestaurant));
    }

    @Test
    void theFilterRefusesToTurnOnOutsideATransaction() {
        assertThatThrownBy(() -> filter.enable(new EstablishmentId(sakura)))
                .isInstanceOf(IllegalStateException.class);
    }
}