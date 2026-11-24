package br.com.cardapio.adapter.out.persistence;

import br.com.cardapio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Toda conexão planeja com os valores de verdade (C3): o plano genérico do Postgres, que vem depois de
 * algumas execuções da mesma consulta, deixava "Pedidos juntos" com 12 meses rodando por minutos.
 */
@IntegrationTest
class PlanCacheModeTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void everyConnectionForcesCustomPlans() {
        assertThat(jdbc.queryForObject("SHOW plan_cache_mode", String.class)).isEqualTo("force_custom_plan");
    }
}
