package br.com.cardapio.domain.report.model;

/** Quantos chamados uma mesa fez e quantas comandas fecharam nela no período (REL-44). */
public record TableCalls(String label, String sector, int calls, int tabs) {

    /** Chamados por comanda; zero sem comandas. */
    public double perTab() {
        return tabs == 0 ? 0 : (double) calls / tabs;
    }
}
