package br.com.cardapio.domain.report.model;

import br.com.cardapio.domain.menu.model.ProductId;

import java.time.Duration;
import java.util.Objects;

/**
 * Um item que ficou pronto: quanto tempo levou da chegada à cozinha até pronto, e o dia da semana e a
 * hora (no fuso do restaurante) em que chegou. A chegada é a confirmação do garçom, quando o pedido
 * precisou dela; senão, o próprio pedido.
 *
 * @param isoWeekday 1 = segunda … 7 = domingo
 * @param hour       0 a 23
 */
public record PrepTime(ProductId productId, String name, String category, int isoWeekday, int hour, Duration time) {

    public PrepTime {
        Objects.requireNonNull(productId, "produto é obrigatório");
        Objects.requireNonNull(time, "tempo é obrigatório");
        if (isoWeekday < 1 || isoWeekday > 7 || hour < 0 || hour > 23) {
            throw new IllegalArgumentException("dia da semana ou hora fora do intervalo");
        }
        if (time.isNegative()) {
            throw new IllegalArgumentException("tempo de preparo negativo");
        }
    }
}
