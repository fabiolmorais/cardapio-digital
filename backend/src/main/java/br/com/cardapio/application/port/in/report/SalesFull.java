package br.com.cardapio.application.port.in.report;

import br.com.cardapio.domain.report.model.AdvancePayments;
import br.com.cardapio.domain.report.model.CancelReasonTotal;
import br.com.cardapio.domain.report.model.PerPersonTicket;
import br.com.cardapio.domain.report.model.SectorSales;
import br.com.cardapio.domain.report.model.TableSales;
import br.com.cardapio.domain.shared.model.Money;

import java.util.List;

/**
 * Os blocos completos de Vendas (Business, REL-04, 06, 07, 11, 12).
 *
 * @param days     os dias do período (a base do "por mesa, por dia")
 * @param reasons  os motivos que mais cancelaram e, por último (reason null), os outros
 */
public record SalesFull(List<SectorSales> bySector, int days, List<TableSales> topTables, PerPersonTicket rodizio,
                        PerPersonTicket comparedRodizio, PerPersonTicket people, PerPersonTicket comparedPeople,
                        AdvancePayments advance, Money canceled, Money comparedCanceled,
                        List<CancelReasonTotal> reasons) {
}
