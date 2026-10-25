package br.com.cardapio.adapter.in.web.plan;

import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Planos são dados globais (sem establishment_id), então não há teste de acesso entre restaurantes aqui.
 */
@IntegrationTest
class PlanControllerTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void listsActivePlansInDisplayOrderWithoutAuthentication() {
        var result = mvc.get().uri("/api/v1/plans");

        assertThat(result).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
        assertThat(result).bodyJson().extractingPath("$.content[*].code")
                .asArray().containsExactly("essential", "pro", "business", "enterprise");
        assertThat(result).bodyJson().extractingPath("$.totalElements").asNumber().isEqualTo(4);
    }

    @Test
    void mapsPricesLimitsAndFeatures() {
        var result = mvc.get().uri("/api/v1/plans");

        assertThat(result).bodyJson().extractingPath("$.content[0].monthlyPriceCents").asNumber().isEqualTo(6990);
        assertThat(result).bodyJson().extractingPath("$.content[0].maxTables").asNumber().isEqualTo(20);
        assertThat(result).bodyJson().extractingPath("$.content[0].historyDays").asNumber().isEqualTo(90);
        assertThat(result).bodyJson().extractingPath("$.content[0].features").asArray().isEmpty();
        assertThat(result).bodyJson().extractingPath("$.content[1].maxUsers").isNull();
        assertThat(result).bodyJson().extractingPath("$.content[1].features").asArray().contains("PRODUCT_VIDEOS");
        assertThat(result).bodyJson().extractingPath("$.content[3].monthlyPriceCents").isNull();
        assertThat(result).bodyJson().extractingPath("$.content[3].historyDays").isNull();
        assertThat(result).bodyJson().extractingPath("$.content[3].support").isEqualTo("DEDICATED_MANAGER");
    }

    @Test
    void reportsAreOnlyInBusinessAndEnterprise() {
        var result = mvc.get().uri("/api/v1/plans");

        assertThat(result).bodyJson().extractingPath("$.content[0].features").asArray().doesNotContain("REPORTS");
        assertThat(result).bodyJson().extractingPath("$.content[1].features").asArray()
                .doesNotContain("REPORTS", "REPORT_EXPORT");
        assertThat(result).bodyJson().extractingPath("$.content[2].features").asArray()
                .contains("REPORTS", "REPORT_EXPORT");
        assertThat(result).bodyJson().extractingPath("$.content[3].features").asArray()
                .contains("REPORTS", "REPORT_EXPORT");
    }

    @Test
    void paginates() {
        var result = mvc.get().uri("/api/v1/plans?page=1&size=3");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content[*].code").asArray().containsExactly("enterprise");
        assertThat(result).bodyJson().extractingPath("$.totalPages").asNumber().isEqualTo(2);
    }

    @Test
    void rejectsInvalidPageSizeWithProblemDetails() {
        var result = mvc.get().uri("/api/v1/plans?size=1000");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").asNumber().isEqualTo(400);
        assertThat(result).bodyJson().doesNotHavePath("$.trace");
    }
}
