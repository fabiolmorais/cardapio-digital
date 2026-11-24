package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.order.model.TabId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * O que passou do limite agora: quantos (itens, chamados ou comandas), a mesa do mais antigo e o
 * começo do mais novo deles (o último a passar do limite cruzou em {@code newestStart} + limite).
 *
 * @param tabId a comanda do mais antigo, no alerta da comanda (para o toque abrir ela)
 */
public record AlertFinding(AlertType type, int count, String oldestTable, Instant newestStart,
                           Optional<TabId> tabId) {

    public AlertFinding {
        Objects.requireNonNull(type, "tipo é obrigatório");
        Objects.requireNonNull(oldestTable, "mesa é obrigatória");
        Objects.requireNonNull(newestStart, "início é obrigatório");
        Objects.requireNonNull(tabId, "comanda é obrigatória");
    }
}
