package br.com.cardapio.application.port.out.order;

import java.time.Instant;

/** Uma mudança de mesa, como o detalhe da comanda mostra. */
public record TabMoveView(String fromTableLabel, String toTableLabel, boolean merged, String movedByName,
                          Instant movedAt) {
}
