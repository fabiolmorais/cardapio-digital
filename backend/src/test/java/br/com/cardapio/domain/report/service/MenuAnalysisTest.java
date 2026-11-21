package br.com.cardapio.domain.report.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MenuAnalysisTest {

    @Test
    void onlyProductsOlderThanThirtyDaysAreSlowSellerCandidates() {
        assertThat(MenuAnalysis.slowSellerCutoff(Instant.parse("2026-11-21T15:00:00Z")))
                .isEqualTo(Instant.parse("2026-10-22T15:00:00Z"));
        assertThat(MenuAnalysis.SLOW_SELLERS).isEqualTo(5);
    }
}
