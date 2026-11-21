package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.ReportLevel;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * O que a tela dos relatórios precisa saber antes de pedir qualquer número.
 *
 * @param level         nenhum (Essencial), básicos (Pro) ou completos (Business)
 * @param canExport     o "Exportar" funciona
 * @param timeZone      o fuso do restaurante: o "dia" dos relatórios
 * @param today         hoje, no fuso do restaurante
 * @param earliestDay   o primeiro dia que o plano deixa consultar; null = sem limite
 * @param firstDataDay  o dia da primeira comanda fechada; null = ainda não há dados
 * @param maxPeriodDays o maior período de uma vez
 */
public record ReportContext(ReportLevel level, boolean canExport, ZoneId timeZone, LocalDate today,
                            LocalDate earliestDay, LocalDate firstDataDay, int maxPeriodDays) {
}
