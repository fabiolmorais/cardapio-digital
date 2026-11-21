package br.com.cardapio.domain.report.model;

/**
 * O quanto dos relatórios o plano libera (ADR 0013).
 * {@code NONE}: Essencial, sem relatórios. {@code BASIC}: Pro, os básicos (vendas, cardápio,
 * picos e tempos de preparo). {@code FULL}: Business e Enterprise, todos.
 */
public enum ReportLevel {
    NONE,
    BASIC,
    FULL;

    /** Este nível vê os blocos de {@code required}? */
    public boolean covers(ReportLevel required) {
        return compareTo(required) >= 0;
    }
}
