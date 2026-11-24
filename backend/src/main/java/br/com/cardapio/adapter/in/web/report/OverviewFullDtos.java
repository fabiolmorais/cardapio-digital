package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.in.report.OverviewFull;
import br.com.cardapio.domain.report.model.GoalProgress;
import br.com.cardapio.domain.shared.model.Money;
import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.OperationTotals;
import br.com.cardapio.domain.report.model.TodayForecast;
import br.com.cardapio.domain.report.model.WaiterCalls;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** Os blocos completos da Visão geral (Business). */
final class OverviewFullDtos {

    private OverviewFullDtos() {
    }

    private static Long seconds(Optional<Duration> duration) {
        return duration.map(Duration::toSeconds).orElse(null);
    }

    @Schema(name = "ReportOperation", description = "Os números de operação de um período")
    record Operation(@Schema(description = "Do pedido chegar à cozinha até a mesa, em média") Long toTableSeconds,
                     @Schema(requiredMode = REQUIRED) int phoneOrders,
                     @Schema(requiredMode = REQUIRED) int staffOrders,
                     @Schema(description = "A parte dos pedidos feita pelo celular, de 0 a 100") BigDecimal phoneSharePercent,
                     @Schema(description = "Do chamado até o garçom aceitar, em média") Long callResponseSeconds,
                     @Schema(description = "Da comanda abrir até fechar, em média") Long staySeconds,
                     @Schema(requiredMode = REQUIRED, description = "Itens cancelados nas comandas fechadas")
                     long canceledCents) {
        static Operation of(OperationTotals o) {
            return new Operation(seconds(o.toTable()), o.phoneOrders(), o.staffOrders(),
                    o.phoneShare().orElse(null), seconds(o.callResponse()), seconds(o.stay()), o.canceled().cents());
        }
    }

    @Schema(name = "ReportToday", description = "Hoje, até agora, contra os mesmos dias da semana anteriores")
    record Today(@Schema(requiredMode = REQUIRED) long soFarCents,
                 @Schema(requiredMode = REQUIRED, description = "A média do dia inteiro") long expectedCents,
                 @Schema(requiredMode = REQUIRED, description = "A média até esta mesma hora") long usualCents,
                 @Schema(requiredMode = REQUIRED, description = "Quantas semanas entraram na média (1 a 4)") int weeks) {
        static Today of(TodayForecast t) {
            return new Today(t.soFar().cents(), t.expected().cents(), t.usual().cents(), t.weeks());
        }
    }

    @Schema(name = "ReportWaiter", description = "Um garçom: chamados aceitos e a resposta média")
    record Waiter(@Schema(requiredMode = REQUIRED) UUID userId, @Schema(requiredMode = REQUIRED) String name,
                  @Schema(requiredMode = REQUIRED) int calls,
                  @Schema(requiredMode = REQUIRED) long averageResponseSeconds) {
        static Waiter of(WaiterCalls w) {
            return new Waiter(w.userId().value(), w.name(), w.calls(), w.averageResponse().toSeconds());
        }
    }

    enum AttentionKind { SLOW_ITEM, FORGOTTEN_TABS, CANCELED_PRODUCT }

    @Schema(name = "ReportAttention", description = """
            Um ponto de atenção. SLOW_ITEM: name, dayAverageSeconds e usualAverageSeconds. FORGOTTEN_TABS:
            tables (as mesas das comandas abertas de um dia para o outro). CANCELED_PRODUCT: name, times e,
            se algum motivo se repetiu, reason e reasonTimes.""")
    record Attention(@Schema(requiredMode = REQUIRED) AttentionKind kind, String name, Long dayAverageSeconds,
                     Long usualAverageSeconds, List<String> tables, Integer times, String reason,
                     Integer reasonTimes) {
        static Attention of(AttentionPoint point) {
            return switch (point) {
                case AttentionPoint.SlowItem s -> new Attention(AttentionKind.SLOW_ITEM, s.item().name(),
                        s.item().dayAverage().toSeconds(), s.item().usualAverage().toSeconds(), null, null, null, null);
                case AttentionPoint.ForgottenTabs f -> new Attention(AttentionKind.FORGOTTEN_TABS, null, null, null,
                        f.tables(), null, null, null);
                case AttentionPoint.CanceledProduct c -> new Attention(AttentionKind.CANCELED_PRODUCT, c.name(), null,
                        null, null, c.times(), c.reason(), c.reason() == null ? null : c.reasonTimes());
            };
        }
    }

    @Schema(name = "OverviewFull", description = "Os blocos do Business")
    record Full(@Schema(requiredMode = REQUIRED) Operation current,
                @Schema(requiredMode = REQUIRED, description = "A comparação; se o período termina hoje, até a mesma hora")
                Operation compared,
                @Schema(description = "Sem histórico do mesmo dia da semana, null") Today today,
                @Schema(requiredMode = REQUIRED) List<Waiter> waiters,
                @Schema(requiredMode = REQUIRED) List<Attention> attention,
                @Schema(description = "A meta do mês de hoje; null sem meta") Goal goal) {
        static Full of(OverviewFull f) {
            return new Full(Operation.of(f.current()), Operation.of(f.compared()), f.today().map(Today::of).orElse(null),
                    f.waiters().stream().map(Waiter::of).toList(), f.attention().stream().map(Attention::of).toList(),
                    f.goal().map(Goal::of).orElse(null));
        }
    }

    @Schema(name = "OverviewGoal", description = "O mês contra a meta de faturamento")
    record Goal(@Schema(requiredMode = REQUIRED, example = "2026-11") String month,
                @Schema(requiredMode = REQUIRED) long goalCents,
                @Schema(requiredMode = REQUIRED, description = "O faturamento do mês até agora") long achievedCents,
                @Schema(requiredMode = REQUIRED, description = "Os dias que faltam, com hoje") int daysLeft,
                @Schema(description = "Quanto por dia bate a meta; null se já bateu") Long perDayNeededCents,
                @Schema(description = "A média por dia dos dias já terminados; null no dia 1") Long dailyAverageCents) {
        static Goal of(GoalProgress g) {
            return new Goal(g.month().toString(), g.goal().cents(), g.achieved().cents(), g.daysLeft(),
                    g.perDayNeeded().map(Money::cents).orElse(null), g.dailyAverage().map(Money::cents).orElse(null));
        }
    }
}
