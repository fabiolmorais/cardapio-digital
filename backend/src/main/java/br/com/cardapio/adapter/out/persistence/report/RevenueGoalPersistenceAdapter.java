package br.com.cardapio.adapter.out.persistence.report;

import br.com.cardapio.application.port.out.report.RevenueGoalPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.report.model.RevenueGoal;
import br.com.cardapio.domain.shared.model.Money;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Optional;

/** As metas de faturamento em SQL parametrizado, sempre por restaurante; o mês é o primeiro dia dele. */
@Component
class RevenueGoalPersistenceAdapter implements RevenueGoalPort {

    private final JdbcClient jdbc;

    RevenueGoalPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RevenueGoal> find(EstablishmentId establishmentId, YearMonth month) {
        return jdbc.sql("""
                        SELECT amount_cents FROM revenue_goal
                        WHERE establishment_id = :establishmentId AND month = :month
                        """)
                .param("establishmentId", establishmentId.value())
                .param("month", month.atDay(1))
                .query((rs, row) -> new RevenueGoal(month, Money.ofCents(rs.getLong("amount_cents"))))
                .optional();
    }

    @Override
    public void save(EstablishmentId establishmentId, YearMonth month, Optional<Money> amount) {
        if (amount.isEmpty()) {
            jdbc.sql("DELETE FROM revenue_goal WHERE establishment_id = :establishmentId AND month = :month")
                    .param("establishmentId", establishmentId.value())
                    .param("month", month.atDay(1))
                    .update();
            return;
        }
        jdbc.sql("""
                        INSERT INTO revenue_goal (establishment_id, month, amount_cents, updated_at)
                        VALUES (:establishmentId, :month, :amount, now())
                        ON CONFLICT (establishment_id, month)
                        DO UPDATE SET amount_cents = EXCLUDED.amount_cents, updated_at = now()
                        """)
                .param("establishmentId", establishmentId.value())
                .param("month", month.atDay(1))
                .param("amount", amount.get().cents())
                .update();
    }
}
