package br.com.cardapio.domain.report.service;

import br.com.cardapio.domain.report.model.CancelReasonTotal;
import br.com.cardapio.domain.shared.model.Money;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** As regras dos blocos completos de Vendas (REL-12). */
public final class SalesFullAnalysis {

    /** Quantos motivos de cancelamento aparecem pelo nome; os outros somam em "Outros". */
    public static final int TOP_REASONS = 3;

    private SalesFullAnalysis() {
    }

    /**
     * Os motivos que mais cancelaram, do maior para o menor (empate: o nome), e depois os outros num
     * grupo só (reason null), se houver. Cancelamento sem motivo entra nos outros.
     */
    public static List<CancelReasonTotal> topReasons(Collection<CancelReasonTotal> reasons) {
        List<CancelReasonTotal> named = reasons.stream()
                .filter(r -> r.reason() != null && !r.reason().isBlank() && r.amount().cents() > 0)
                .sorted(Comparator.comparingLong((CancelReasonTotal r) -> r.amount().cents()).reversed()
                        .thenComparing(CancelReasonTotal::reason))
                .toList();
        List<CancelReasonTotal> top = new ArrayList<>(named.subList(0, Math.min(TOP_REASONS, named.size())));
        long others = reasons.stream().mapToLong(r -> r.amount().cents()).sum()
                - top.stream().mapToLong(r -> r.amount().cents()).sum();
        if (others > 0) {
            top.add(new CancelReasonTotal(null, Money.ofCents(others)));
        }
        return top;
    }

    /** O total cancelado: a soma de todos os motivos. */
    public static Money total(Collection<CancelReasonTotal> reasons) {
        return Money.ofCents(reasons.stream().mapToLong(r -> r.amount().cents()).sum());
    }
}
