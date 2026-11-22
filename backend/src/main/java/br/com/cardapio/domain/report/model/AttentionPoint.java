package br.com.cardapio.domain.report.model;

import java.util.List;

/** Um ponto de atenção da Visão geral (REL-33, 61, 23): o que vale olhar, sem precisar procurar. */
public sealed interface AttentionPoint {

    /** Um item que demorou bem mais que o normal no período. */
    record SlowItem(SlowAttention item) implements AttentionPoint {
    }

    /** Comandas que ficaram abertas de um dia para o outro (ainda abertas, abertas antes de hoje). */
    record ForgottenTabs(List<String> tables) implements AttentionPoint {

        public ForgottenTabs {
            tables = List.copyOf(tables);
        }
    }

    /**
     * O produto mais cancelado do período, com o motivo mais comum.
     *
     * @param reason o motivo mais repetido; null se nenhum se repetiu
     */
    record CanceledProduct(String name, int times, String reason, int reasonTimes) implements AttentionPoint {
    }
}
