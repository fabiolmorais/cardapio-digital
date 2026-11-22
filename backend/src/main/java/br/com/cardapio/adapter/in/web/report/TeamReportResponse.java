package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.adapter.in.web.report.ReportDtos.Range;
import br.com.cardapio.application.port.in.report.TeamReport;
import br.com.cardapio.domain.report.model.CallTotals;
import br.com.cardapio.domain.report.model.CallTypeCount;
import br.com.cardapio.domain.report.model.HourDuration;
import br.com.cardapio.domain.report.model.ShiftLoad;
import br.com.cardapio.domain.report.model.TableCalls;
import br.com.cardapio.domain.report.model.WaiterStats;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(name = "TeamReport", description = "Atendimento e equipe (Business)")
record TeamReportResponse(
        @Schema(requiredMode = REQUIRED) Range period,
        @Schema(requiredMode = REQUIRED) Range comparison,
        @Schema(requiredMode = REQUIRED) Calls current,
        @Schema(requiredMode = REQUIRED, description = "A comparação; se o período termina hoje, até a mesma hora")
        Calls compared,
        @Schema(requiredMode = REQUIRED, description = "Quem fez algo no período, de quem mais atendeu chamados")
        List<Waiter> waiters,
        @Schema(requiredMode = REQUIRED) List<Hour> responseByHour,
        @Schema(requiredMode = REQUIRED) List<CallType> byType,
        @Schema(requiredMode = REQUIRED, description = "As que mais chamam por comanda (3 comandas ou mais)")
        List<Table> topTables,
        @Schema(description = "Chamados por comanda do restaurante todo; null sem comandas") Double tableAverage,
        @Schema(requiredMode = REQUIRED, description = "Avisos de pedido repetido mostrados") int warningsShown,
        @Schema(requiredMode = REQUIRED, description = "Os que o garçom confirmou") int warningsConfirmed,
        @Schema(requiredMode = REQUIRED, description = "As horas com mais chamados por pessoa do salão no turno")
        List<Load> busiestShifts) {

    private static Long seconds(Optional<Duration> d) {
        return d.map(Duration::toSeconds).orElse(null);
    }

    @Schema(name = "ReportCalls", description = "Os chamados do período")
    record Calls(@Schema(requiredMode = REQUIRED) int calls,
                 @Schema(description = "Até o garçom aceitar, em média") Long responseSeconds,
                 @Schema(description = "Até concluir, em média") Long doneSeconds,
                 @Schema(description = "De pedir a conta até a comanda fechar, em média") Long billToCloseSeconds) {
        static Calls of(CallTotals c) {
            return new Calls(c.calls(), seconds(c.response()), seconds(c.done()), seconds(c.billToClose()));
        }
    }

    @Schema(name = "ReportTeamMember", description = "O que uma pessoa da equipe fez no período")
    record Waiter(@Schema(requiredMode = REQUIRED) UUID userId, @Schema(requiredMode = REQUIRED) String name,
                  @Schema(requiredMode = REQUIRED) int calls, Long responseSeconds,
                  @Schema(requiredMode = REQUIRED) int delivered, @Schema(requiredMode = REQUIRED) int closedTabs,
                  @Schema(requiredMode = REQUIRED, description = "O faturamento das comandas que fechou")
                  long revenueCents,
                  @Schema(requiredMode = REQUIRED) int staffOrders,
                  @Schema(requiredMode = REQUIRED, description = "A taxa de serviço destinada a ela") long tipsCents) {
        static Waiter of(WaiterStats w) {
            return new Waiter(w.userId().value(), w.name(), w.calls(), seconds(w.response()), w.delivered(),
                    w.closedTabs(), w.revenue().cents(), w.staffOrders(), w.tips().cents());
        }
    }

    @Schema(name = "ReportCallHour", description = "A resposta média ao chamado numa hora do dia")
    record Hour(@Schema(requiredMode = REQUIRED) int hour, @Schema(requiredMode = REQUIRED) long responseSeconds,
                @Schema(requiredMode = REQUIRED) int calls) {
        static Hour of(HourDuration h) {
            return new Hour(h.hour(), h.average().toSeconds(), h.count());
        }
    }

    enum Type { WAITER, BILL, UNLOCK, RODIZIO }

    @Schema(name = "ReportCallType", description = "Os chamados de um tipo e os cancelados pelo cliente")
    record CallType(@Schema(requiredMode = REQUIRED) Type type, @Schema(requiredMode = REQUIRED) int calls,
                    @Schema(requiredMode = REQUIRED) int canceled) {
        static CallType of(CallTypeCount c) {
            return new CallType(Type.valueOf(c.type().name()), c.calls(), c.canceled());
        }
    }

    @Schema(name = "ReportTableCalls", description = "Uma mesa: chamados e comandas fechadas")
    record Table(@Schema(requiredMode = REQUIRED) String label, @Schema(requiredMode = REQUIRED) String sector,
                 @Schema(requiredMode = REQUIRED) int calls, @Schema(requiredMode = REQUIRED) int tabs) {
        static Table of(TableCalls t) {
            return new Table(t.label(), t.sector(), t.calls(), t.tabs());
        }
    }

    @Schema(name = "ReportShiftLoad", description = "Uma hora de um dia: chamados e pessoas do salão no turno")
    record Load(@Schema(requiredMode = REQUIRED) LocalDate day, @Schema(requiredMode = REQUIRED) int hour,
                @Schema(requiredMode = REQUIRED) int calls, @Schema(requiredMode = REQUIRED) int waiters) {
        static Load of(ShiftLoad l) {
            return new Load(l.day(), l.hour(), l.calls(), l.waiters());
        }
    }

    static TeamReportResponse from(TeamReport r) {
        return new TeamReportResponse(Range.of(r.period()), Range.of(r.comparison()), Calls.of(r.current()),
                Calls.of(r.compared()), r.waiters().stream().map(Waiter::of).toList(),
                r.responseByHour().stream().map(Hour::of).toList(), r.byType().stream().map(CallType::of).toList(),
                r.topTables().stream().map(Table::of).toList(), r.tableAverage().orElse(null), r.warnings().shown(),
                r.warnings().confirmed(), r.busiestShifts().stream().map(Load::of).toList());
    }
}
