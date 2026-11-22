package br.com.cardapio.domain.report.model;

/**
 * Os avisos de pedido repetido do período (REL-50): mostrados e confirmados. O que não foi confirmado,
 * o garçom tirou (ou desistiu do envio).
 */
public record DuplicateWarnings(int shown, int confirmed) {

    public int removed() {
        return shown - confirmed;
    }
}
