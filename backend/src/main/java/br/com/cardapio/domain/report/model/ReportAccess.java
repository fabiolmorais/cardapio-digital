package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.plan.model.Plan;
import br.com.cardapio.domain.plan.model.PlanFeature;
import br.com.cardapio.domain.report.exception.ReportPeriodOutOfRangeException;
import br.com.cardapio.domain.report.exception.ReportsNotInPlanException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * O que o plano libera nos relatórios (ADR 0013): o nível, a exportação e quanto do passado dá
 * para consultar. O back decide; o front só mostra o que veio.
 *
 * @param level       nenhum, básicos ou completos
 * @param canExport   exporta em CSV
 * @param historyDays quantos dias para trás dá para consultar, contando hoje; 0 = sem limite
 */
public record ReportAccess(ReportLevel level, boolean canExport, int historyDays) {

    public ReportAccess {
        Objects.requireNonNull(level, "nível é obrigatório");
        if (historyDays < 0) {
            throw new IllegalArgumentException("dias de histórico não pode ser negativo");
        }
    }

    public static ReportAccess of(Plan plan) {
        ReportLevel level = plan.includes(PlanFeature.REPORTS) ? ReportLevel.FULL
                : plan.includes(PlanFeature.BASIC_REPORTS) ? ReportLevel.BASIC
                : ReportLevel.NONE;
        return new ReportAccess(level, plan.includes(PlanFeature.REPORT_EXPORT), plan.historyDays());
    }

    /** O primeiro dia que o plano deixa consultar; vazio quando não há limite. */
    public Optional<LocalDate> earliestDay(LocalDate today) {
        return historyDays == 0 ? Optional.empty() : Optional.of(today.minusDays(historyDays - 1L));
    }

    /** Sem relatórios no plano (Essencial): ReportsNotInPlanException. */
    public void requireReports() {
        if (level == ReportLevel.NONE) {
            throw new ReportsNotInPlanException("Os relatórios começam no plano Pro");
        }
    }

    public void requireExport() {
        if (!canExport) {
            throw new ReportsNotInPlanException("A exportação dos relatórios é do plano Business");
        }
    }

    /**
     * O período cabe no plano: termina até hoje e não começa antes do histórico liberado. A
     * comparação com o período anterior pode passar do histórico: é só a base do "▲ 9%".
     */
    public void checkPeriod(ReportPeriod period, LocalDate today) {
        requireReports();
        if (period.to().isAfter(today)) {
            throw new ReportPeriodOutOfRangeException("O período não pode passar de hoje");
        }
        earliestDay(today).filter(period.from()::isBefore).ifPresent(earliest -> {
            throw new ReportPeriodOutOfRangeException(
                    "Seu plano mostra os relatórios dos últimos " + historyDays / 30 + " meses");
        });
    }
}
