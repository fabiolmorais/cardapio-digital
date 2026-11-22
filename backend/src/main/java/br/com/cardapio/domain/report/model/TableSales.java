package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.floor.model.DiningTableId;
import br.com.cardapio.domain.shared.model.Money;

/** Uma mesa no período (REL-07): quantas comandas fecharam nela e quanto faturaram. */
public record TableSales(DiningTableId tableId, String label, String sector, int tabs, Money revenue) {

    public Money averageTicket() {
        return new ReportTotals(revenue, tabs).averageTicket();
    }
}
